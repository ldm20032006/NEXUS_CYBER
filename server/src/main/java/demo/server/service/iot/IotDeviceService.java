package demo.server.service.iot;

import demo.server.common.audit.AuditRecorder;
import demo.server.common.enums.AlertSeverity;
import demo.server.common.enums.AlertStatus;
import demo.server.common.enums.AuditAction;
import demo.server.common.enums.DeviceStatus;
import demo.server.common.event.DomainEventEnvelopeFactory;
import demo.server.common.event.DomainEventPublisher;
import demo.server.common.resilience.DistributedLockService;
import demo.server.common.resilience.LockHandle;
import demo.server.common.resilience.ResilienceKeys;
import demo.server.common.time.ClockProvider;
import demo.server.common.websocket.WebSocketEventPublisher;
import demo.server.common.websocket.WebSocketTopics;
import demo.server.dto.iot.DeviceHeartbeatRequest;
import demo.server.dto.iot.DeviceTelemetryRequest;
import demo.server.dto.iot.DeviceTelemetryResponse;
import demo.server.dto.iot.IotDeviceRequest;
import demo.server.dto.iot.IotDeviceResponse;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import demo.server.entity.iot.DeviceTelemetry;
import demo.server.entity.iot.IotDevice;
import demo.server.exception.BusinessRuleException;
import demo.server.exception.ConcurrencyConflictException;
import demo.server.exception.DuplicateResourceException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.repository.branch.BranchRepository;
import demo.server.repository.branch.StationRepository;
import demo.server.repository.iot.DeviceTelemetryRepository;
import demo.server.repository.iot.IotDeviceRepository;
import demo.server.service.branch.BranchScope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class IotDeviceService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final String HEARTBEAT_MISSED_CODE = "HEARTBEAT_MISSED";

    private final IotDeviceRepository deviceRepository;
    private final DeviceTelemetryRepository telemetryRepository;
    private final BranchRepository branchRepository;
    private final StationRepository stationRepository;
    private final BranchScope branchScope;
    private final DistributedLockService lockService;
    private final ResilienceKeys resilienceKeys;
    private final ClockProvider clockProvider;
    private final IotMapper mapper;
    private final AuditRecorder auditRecorder;
    private final DomainEventPublisher domainEventPublisher;
    private final DomainEventEnvelopeFactory envelopeFactory;
    private final WebSocketEventPublisher webSocketEventPublisher;
    private final DeviceAlertService alertService;

    public IotDeviceService(IotDeviceRepository deviceRepository, DeviceTelemetryRepository telemetryRepository,
                            BranchRepository branchRepository, StationRepository stationRepository,
                            BranchScope branchScope, DistributedLockService lockService,
                            ResilienceKeys resilienceKeys, ClockProvider clockProvider,
                            IotMapper mapper, AuditRecorder auditRecorder, DomainEventPublisher domainEventPublisher,
                            DomainEventEnvelopeFactory envelopeFactory, WebSocketEventPublisher webSocketEventPublisher,
                            DeviceAlertService alertService) {
        this.deviceRepository = deviceRepository;
        this.telemetryRepository = telemetryRepository;
        this.branchRepository = branchRepository;
        this.stationRepository = stationRepository;
        this.branchScope = branchScope;
        this.lockService = lockService;
        this.resilienceKeys = resilienceKeys;
        this.clockProvider = clockProvider;
        this.mapper = mapper;
        this.auditRecorder = auditRecorder;
        this.domainEventPublisher = domainEventPublisher;
        this.envelopeFactory = envelopeFactory;
        this.webSocketEventPublisher = webSocketEventPublisher;
        this.alertService = alertService;
    }

    @Transactional
    public IotDeviceResponse create(IotDeviceRequest request) {
        Branch branch = branch(request.branchId());
        branchScope.assertBranchAllowed(branch.getId());
        if (deviceRepository.existsBySerialNumber(normalizeSerial(request.serialNumber()))) {
            throw new DuplicateResourceException("Device serial already exists");
        }
        Station station = stationInBranch(request.stationId(), branch);
        IotDevice device = new IotDevice();
        device.setBranch(branch);
        device.setStation(station);
        apply(device, request);
        IotDevice saved = deviceRepository.save(device);
        auditRecorder.record(AuditAction.CREATE_DEVICE, "IotDevice", saved.getId(), null, mapper.toDevice(saved));
        publish(saved, "IOT_DEVICE_CREATED");
        return mapper.toDevice(saved);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<IotDeviceResponse> list(UUID branchId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        UUID scopedBranchId = branchScope.requireScopedBranch(branchId);
        if (scopedBranchId == null) {
            return deviceRepository.findByDeletedFalse(pageable).map(mapper::toDevice);
        }
        return deviceRepository.findByBranchIdAndDeletedFalse(scopedBranchId, pageable).map(mapper::toDevice);
    }

    @Transactional(readOnly = true)
    public IotDeviceResponse get(UUID id) {
        IotDevice device = device(id);
        branchScope.assertBranchAllowed(device.getBranch().getId());
        return mapper.toDevice(device);
    }

    @Transactional
    public IotDeviceResponse update(UUID id, IotDeviceRequest request) {
        IotDevice device = device(id);
        branchScope.assertBranchAllowed(device.getBranch().getId());
        if (!device.getBranch().getId().equals(request.branchId())) {
            branchScope.assertBranchAllowed(request.branchId());
        }
        Branch branch = branch(request.branchId());
        Station station = stationInBranch(request.stationId(), branch);
        String normalized = normalizeSerial(request.serialNumber());
        deviceRepository.findBySerialNumber(normalized)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Device serial already exists");
                });
        IotDeviceResponse before = mapper.toDevice(device);
        device.setBranch(branch);
        device.setStation(station);
        apply(device, request);
        auditRecorder.record(AuditAction.UPDATE_DEVICE, "IotDevice", device.getId(), before, mapper.toDevice(device));
        publish(device, "IOT_DEVICE_UPDATED");
        return mapper.toDevice(device);
    }

    @Transactional
    public void delete(UUID id) {
        IotDevice device = device(id);
        branchScope.assertBranchAllowed(device.getBranch().getId());
        device.softDelete();
        device.setStatus(DeviceStatus.DISABLED);
        auditRecorder.record(AuditAction.UPDATE_DEVICE, "IotDevice", device.getId(), null, mapper.toDevice(device));
        publish(device, "IOT_DEVICE_DISABLED");
    }

    @Transactional
    public IotDeviceResponse heartbeat(UUID id, DeviceHeartbeatRequest request) {
        try (LockHandle ignored = lockService.tryAcquire(resilienceKeys.lockDevice(id), LOCK_TTL)
                .orElseThrow(() -> new ConcurrencyConflictException("Device is locked"))) {
            IotDevice device = deviceRepository.findWithLockById(id)
                    .filter(item -> !item.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
            branchScope.assertBranchAllowed(device.getBranch().getId());
            device.setLastHeartbeatAt(clockProvider.now());
            device.setMissedHeartbeatCount(0);
            if (device.getStatus() != DeviceStatus.DISABLED && device.getStatus() != DeviceStatus.MAINTENANCE) {
                device.setStatus(DeviceStatus.ONLINE);
            }
            if (StringUtils.hasText(request.firmwareVersion())) {
                device.setFirmwareVersion(request.firmwareVersion());
            }
            if (StringUtils.hasText(request.ipAddress())) {
                device.setIpAddress(request.ipAddress());
            }
            auditRecorder.record(AuditAction.DEVICE_HEARTBEAT, "IotDevice", device.getId(), null, mapper.toDevice(device));
            publish(device, "IOT_DEVICE_HEARTBEAT");
            return mapper.toDevice(device);
        }
    }

    @Transactional
    public IotDeviceResponse missedHeartbeat(UUID id) {
        try (LockHandle ignored = lockService.tryAcquire(resilienceKeys.lockDevice(id), LOCK_TTL)
                .orElseThrow(() -> new ConcurrencyConflictException("Device is locked"))) {
            IotDevice device = deviceRepository.findWithLockById(id)
                    .filter(item -> !item.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
            branchScope.assertBranchAllowed(device.getBranch().getId());
            device.setMissedHeartbeatCount(device.getMissedHeartbeatCount() + 1);
            if (device.getMissedHeartbeatCount() >= 3 && device.getStatus() != DeviceStatus.OFFLINE) {
                device.setStatus(DeviceStatus.OFFLINE);
                alertService.createSystemAlert(device, HEARTBEAT_MISSED_CODE, "Device missed heartbeat",
                        "Device missed 3 consecutive heartbeat checks", AlertSeverity.HIGH, false);
            }
            auditRecorder.record(AuditAction.DEVICE_HEARTBEAT, "IotDevice", device.getId(), null, mapper.toDevice(device));
            publish(device, "IOT_DEVICE_HEARTBEAT_MISSED");
            return mapper.toDevice(device);
        }
    }

    @Transactional
    public DeviceTelemetryResponse recordTelemetry(UUID deviceId, DeviceTelemetryRequest request) {
        IotDevice device = device(deviceId);
        branchScope.assertBranchAllowed(device.getBranch().getId());
        DeviceTelemetry telemetry = new DeviceTelemetry();
        telemetry.setDevice(device);
        telemetry.setBranch(device.getBranch());
        telemetry.setStation(device.getStation());
        telemetry.setReceivedAt(clockProvider.now());
        telemetry.setOnline(request.online());
        telemetry.setBatteryLevel(request.batteryLevel());
        telemetry.setSignalStrength(request.signalStrength());
        telemetry.setErrorCode(request.errorCode());
        telemetry.setFirmwareVersion(request.firmwareVersion());
        telemetry.setMetricKey(request.metricKey());
        telemetry.setMetricValue(request.metricValue());
        telemetry.setPayloadJson(request.payloadJson());
        DeviceTelemetry saved = telemetryRepository.save(telemetry);
        auditRecorder.record(AuditAction.DEVICE_TELEMETRY, "DeviceTelemetry", saved.getId(), null, mapper.toTelemetry(saved));
        publishTelemetry(saved);
        return mapper.toTelemetry(saved);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<DeviceTelemetryResponse> telemetry(UUID deviceId, Instant from, Instant to, int page, int size, Integer limit) {
        IotDevice device = device(deviceId);
        branchScope.assertBranchAllowed(device.getBranch().getId());
        int resolvedSize = limit == null ? size : Math.min(size, limit);
        Pageable pageable = PageRequest.of(page, Math.min(resolvedSize, 500));
        Instant resolvedFrom = from == null ? Instant.EPOCH : from;
        Instant resolvedTo = to == null ? clockProvider.now() : to;
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new BusinessRuleException("Telemetry from must be before to");
        }
        return telemetryRepository.findByDeviceIdAndReceivedAtBetweenOrderByReceivedAtDesc(deviceId, resolvedFrom, resolvedTo, pageable)
                .map(mapper::toTelemetry);
    }

    private void apply(IotDevice device, IotDeviceRequest request) {
        device.setDeviceType(request.deviceType());
        device.setSerialNumber(normalizeSerial(request.serialNumber()));
        device.setName(request.name());
        device.setFirmwareVersion(request.firmwareVersion());
        device.setCapabilities(request.capabilities());
        device.setStatus(request.status() == null ? DeviceStatus.ACTIVE : request.status());
        device.setIpAddress(request.ipAddress());
    }

    private Branch branch(UUID id) {
        return branchRepository.findById(id).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
    }

    private Station stationInBranch(UUID stationId, Branch branch) {
        if (stationId == null) {
            return null;
        }
        Station station = stationRepository.findById(stationId).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Station not found"));
        if (!station.getBranch().getId().equals(branch.getId())) {
            throw new BusinessRuleException("Device station must belong to the same branch");
        }
        return station;
    }

    private IotDevice device(UUID id) {
        return deviceRepository.findById(id).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
    }

    private String normalizeSerial(String serialNumber) {
        return serialNumber.trim().toUpperCase();
    }

    private void publish(IotDevice device, String eventType) {
        IotDeviceResponse response = mapper.toDevice(device);
        domainEventPublisher.publishAfterCommit(envelopeFactory.create(eventType, 1, Map.of("deviceId", device.getId().toString())));
        webSocketEventPublisher.sendAfterCommit(WebSocketTopics.branchAlerts(device.getBranch().getId()), eventType, 1, response);
        if (device.getStation() != null) {
            webSocketEventPublisher.sendAfterCommit(WebSocketTopics.station(device.getStation().getId()), eventType, 1, response);
        }
    }

    private void publishTelemetry(DeviceTelemetry telemetry) {
        DeviceTelemetryResponse response = mapper.toTelemetry(telemetry);
        domainEventPublisher.publishAfterCommit(envelopeFactory.create("IOT_DEVICE_TELEMETRY", 1, Map.of("telemetryId", telemetry.getId().toString())));
        webSocketEventPublisher.sendAfterCommit(WebSocketTopics.branchAlerts(telemetry.getBranch().getId()), "IOT_DEVICE_TELEMETRY", 1, response);
        if (telemetry.getStation() != null) {
            webSocketEventPublisher.sendAfterCommit(WebSocketTopics.station(telemetry.getStation().getId()), "IOT_DEVICE_TELEMETRY", 1, response);
        }
    }
}
