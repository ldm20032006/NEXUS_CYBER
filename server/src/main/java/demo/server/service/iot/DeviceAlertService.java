package demo.server.service.iot;

import demo.server.common.audit.AuditRecorder;
import demo.server.common.enums.AlertSeverity;
import demo.server.common.enums.AlertStatus;
import demo.server.common.enums.AuditAction;
import demo.server.common.enums.RoleCode;
import demo.server.common.event.DomainEventEnvelopeFactory;
import demo.server.common.event.DomainEventPublisher;
import demo.server.common.resilience.DistributedLockService;
import demo.server.common.resilience.LockHandle;
import demo.server.common.resilience.ResilienceKeys;
import demo.server.common.security.CurrentUserProvider;
import demo.server.common.time.ClockProvider;
import demo.server.common.websocket.WebSocketEventPublisher;
import demo.server.common.websocket.WebSocketTopics;
import demo.server.dto.iot.AlertHistoryResponse;
import demo.server.dto.iot.AssignDeviceAlertRequest;
import demo.server.dto.iot.DeviceAlertRequest;
import demo.server.dto.iot.DeviceAlertResponse;
import demo.server.dto.iot.DeviceAlertStatusRequest;
import demo.server.entity.auth.AppUser;
import demo.server.entity.iot.AlertHistory;
import demo.server.entity.iot.DeviceAlert;
import demo.server.entity.iot.IotDevice;
import demo.server.exception.BusinessRuleException;
import demo.server.exception.ConcurrencyConflictException;
import demo.server.exception.DuplicateResourceException;
import demo.server.exception.ForbiddenException;
import demo.server.exception.InvalidTransitionException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.exception.UnauthorizedException;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.iot.AlertHistoryRepository;
import demo.server.repository.iot.DeviceAlertRepository;
import demo.server.repository.iot.IotDeviceRepository;
import demo.server.service.branch.BranchScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DeviceAlertService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final List<AlertStatus> OPEN_STATUSES = List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED, AlertStatus.IN_PROGRESS, AlertStatus.REOPENED);

    private final DeviceAlertRepository alertRepository;
    private final AlertHistoryRepository historyRepository;
    private final IotDeviceRepository deviceRepository;
    private final AppUserRepository userRepository;
    private final BranchScope branchScope;
    private final CurrentUserProvider currentUserProvider;
    private final DistributedLockService lockService;
    private final ResilienceKeys resilienceKeys;
    private final ClockProvider clockProvider;
    private final IotMapper mapper;
    private final AuditRecorder auditRecorder;
    private final DomainEventPublisher domainEventPublisher;
    private final DomainEventEnvelopeFactory envelopeFactory;
    private final WebSocketEventPublisher webSocketEventPublisher;

    public DeviceAlertService(DeviceAlertRepository alertRepository, AlertHistoryRepository historyRepository,
                              IotDeviceRepository deviceRepository, AppUserRepository userRepository,
                              BranchScope branchScope, CurrentUserProvider currentUserProvider,
                              DistributedLockService lockService, ResilienceKeys resilienceKeys, ClockProvider clockProvider,
                              IotMapper mapper, AuditRecorder auditRecorder, DomainEventPublisher domainEventPublisher,
                              DomainEventEnvelopeFactory envelopeFactory, WebSocketEventPublisher webSocketEventPublisher) {
        this.alertRepository = alertRepository;
        this.historyRepository = historyRepository;
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.branchScope = branchScope;
        this.currentUserProvider = currentUserProvider;
        this.lockService = lockService;
        this.resilienceKeys = resilienceKeys;
        this.clockProvider = clockProvider;
        this.mapper = mapper;
        this.auditRecorder = auditRecorder;
        this.domainEventPublisher = domainEventPublisher;
        this.envelopeFactory = envelopeFactory;
        this.webSocketEventPublisher = webSocketEventPublisher;
    }

    @Transactional
    public DeviceAlertResponse create(DeviceAlertRequest request) {
        IotDevice device = device(request.deviceId());
        branchScope.assertBranchAllowed(device.getBranch().getId());
        DeviceAlert alert = createAlert(device, request.alertCode(), request.title(), request.description(), request.severity(),
                request.severity() == AlertSeverity.CRITICAL, request.note(), actorOrNull());
        return mapper.toAlert(alert);
    }

    @Transactional
    public DeviceAlert createSystemAlert(IotDevice device, String alertCode, String title, String description,
                                         AlertSeverity severity, boolean lockMechanical) {
        String normalizedCode = alertCode.trim().toUpperCase();
        List<DeviceAlert> existing = alertRepository.findOpenDuplicates(device.getId(), normalizedCode, OPEN_STATUSES);
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }
        return createAlert(device, alertCode, title, description, severity, lockMechanical, null, null);
    }

    @Transactional(readOnly = true)
    public Page<DeviceAlertResponse> list(UUID branchId, AlertStatus status, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100));
        UUID scopedBranchId = branchScope.requireScopedBranch(branchId);
        if (scopedBranchId == null) {
            return alertRepository.findByDeletedFalseOrderByCreatedAtDesc(pageable).map(mapper::toAlert);
        }
        if (status == null) {
            return alertRepository.findByBranchIdAndDeletedFalseOrderByCreatedAtDesc(scopedBranchId, pageable).map(mapper::toAlert);
        }
        return alertRepository.findByBranchIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(scopedBranchId, status, pageable).map(mapper::toAlert);
    }

    @Transactional(readOnly = true)
    public DeviceAlertResponse get(UUID id) {
        DeviceAlert alert = alert(id);
        branchScope.assertBranchAllowed(alert.getBranch().getId());
        return mapper.toAlert(alert);
    }

    @Transactional
    public DeviceAlertResponse assign(UUID id, AssignDeviceAlertRequest request) {
        try (LockHandle ignored = lockService.tryAcquire(resilienceKeys.lockDeviceAlert(id), LOCK_TTL)
                .orElseThrow(() -> new ConcurrencyConflictException("Alert is locked"))) {
            DeviceAlert alert = alertForUpdate(id);
            branchScope.assertBranchAllowed(alert.getBranch().getId());
            AppUser staff = userRepository.findById(request.staffId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
            if (staff.getBranch() == null || !staff.getBranch().getId().equals(alert.getBranch().getId())) {
                throw new ForbiddenException("Staff is outside alert branch");
            }
            if (staff.getRoles().stream().noneMatch(role -> Set.of(RoleCode.STAFF_TECHNICAL, RoleCode.BRANCH_ADMIN, RoleCode.SUPER_ADMIN).contains(role.getCode()))) {
                throw new BusinessRuleException("Alert can only be assigned to technical staff or branch admin");
            }
            alert.setAssignedStaff(staff);
            appendHistory(alert, alert.getStatus(), alert.getStatus(), "ASSIGN", request.note(), actorOrNull());
            auditRecorder.record(AuditAction.ASSIGN_DEVICE_ALERT, "DeviceAlert", alert.getId(), null, mapper.toAlert(alert));
            publish(alert, "DEVICE_ALERT_ASSIGNED");
            return mapper.toAlert(alert);
        }
    }

    @Transactional
    public DeviceAlertResponse transition(UUID id, DeviceAlertStatusRequest request) {
        try (LockHandle ignored = lockService.tryAcquire(resilienceKeys.lockDeviceAlert(id), LOCK_TTL)
                .orElseThrow(() -> new ConcurrencyConflictException("Alert is locked"))) {
            DeviceAlert alert = alertForUpdate(id);
            branchScope.assertBranchAllowed(alert.getBranch().getId());
            AlertStatus from = alert.getStatus();
            AlertStatus next = request.status();
            validateTransition(from, next);
            AppUser actor = actorOrNull();
            Instant now = clockProvider.now();
            alert.setStatus(next);
            alert.setNote(request.note());
            if (next == AlertStatus.ACKNOWLEDGED) {
                alert.setAcknowledgedBy(actor);
                alert.setAcknowledgedAt(now);
            } else if (next == AlertStatus.RESOLVED) {
                alert.setResolvedBy(actor);
                alert.setResolvedAt(now);
                if (alert.isCriticalMechanicalLock()) {
                    alert.getDevice().setMechanicalCommandLocked(false);
                }
            } else if (next == AlertStatus.CLOSED) {
                alert.setClosedAt(now);
            }
            appendHistory(alert, from, next, "STATUS_CHANGE", request.note(), actor);
            auditRecorder.record(AuditAction.UPDATE_DEVICE_ALERT, "DeviceAlert", alert.getId(), null, mapper.toAlert(alert));
            publish(alert, "DEVICE_ALERT_STATUS_CHANGED");
            return mapper.toAlert(alert);
        }
    }

    @Transactional(readOnly = true)
    public List<AlertHistoryResponse> history(UUID alertId) {
        DeviceAlert alert = alert(alertId);
        branchScope.assertBranchAllowed(alert.getBranch().getId());
        return historyRepository.findByAlertIdOrderByCreatedAtAsc(alertId).stream().map(mapper::toHistory).toList();
    }

    private DeviceAlert createAlert(IotDevice device, String alertCode, String title, String description,
                                    AlertSeverity severity, boolean lockMechanical, String note, AppUser actor) {
        if (device.isDeleted()) {
            throw new ResourceNotFoundException("Device not found");
        }
        String normalizedCode = alertCode.trim().toUpperCase();
        if (alertRepository.existsByDeviceIdAndAlertCodeAndStatusInAndDeletedFalse(device.getId(), normalizedCode, OPEN_STATUSES)) {
            throw new DuplicateResourceException("Open alert already exists for this device and code");
        }
        DeviceAlert alert = new DeviceAlert();
        alert.setDevice(device);
        alert.setBranch(device.getBranch());
        alert.setStation(device.getStation());
        alert.setAlertCode(normalizedCode);
        alert.setTitle(title);
        alert.setDescription(description);
        alert.setSeverity(severity);
        alert.setStatus(AlertStatus.OPEN);
        alert.setCriticalMechanicalLock(lockMechanical || severity == AlertSeverity.CRITICAL);
        alert.setNote(note);
        if (alert.isCriticalMechanicalLock()) {
            device.setMechanicalCommandLocked(true);
        }
        DeviceAlert saved = alertRepository.save(alert);
        appendHistory(saved, null, AlertStatus.OPEN, "CREATE", note, actor);
        auditRecorder.record(AuditAction.CREATE_DEVICE_ALERT, "DeviceAlert", saved.getId(), null, mapper.toAlert(saved));
        publish(saved, "DEVICE_ALERT_CREATED");
        return saved;
    }

    private void validateTransition(AlertStatus from, AlertStatus next) {
        if (from == next) {
            return;
        }
        boolean valid = (from == AlertStatus.OPEN && (next == AlertStatus.ACKNOWLEDGED || next == AlertStatus.RESOLVED))
                || (from == AlertStatus.ACKNOWLEDGED && (next == AlertStatus.IN_PROGRESS || next == AlertStatus.RESOLVED))
                || (from == AlertStatus.IN_PROGRESS && next == AlertStatus.RESOLVED)
                || (from == AlertStatus.RESOLVED && next == AlertStatus.CLOSED)
                || (from == AlertStatus.CLOSED && next == AlertStatus.REOPENED)
                || (from == AlertStatus.REOPENED && (next == AlertStatus.ACKNOWLEDGED || next == AlertStatus.RESOLVED));
        if (!valid) {
            throw new InvalidTransitionException("Invalid alert status transition");
        }
    }

    private void appendHistory(DeviceAlert alert, AlertStatus from, AlertStatus to, String action, String note, AppUser actor) {
        AlertHistory history = new AlertHistory();
        history.setAlert(alert);
        history.setActor(actor);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setAction(action);
        history.setNote(note);
        historyRepository.save(history);
    }

    private DeviceAlert alert(UUID id) {
        return alertRepository.findById(id).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));
    }

    private DeviceAlert alertForUpdate(UUID id) {
        return alertRepository.findWithLockById(id).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));
    }

    private IotDevice device(UUID id) {
        return deviceRepository.findById(id).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
    }

    private AppUser actorOrNull() {
        UUID actorId = currentUserProvider.currentUserId().orElse(null);
        if (actorId == null) {
            return null;
        }
        return userRepository.findById(actorId).orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }

    private void publish(DeviceAlert alert, String eventType) {
        DeviceAlertResponse response = mapper.toAlert(alert);
        domainEventPublisher.publishAfterCommit(envelopeFactory.create(eventType, 1, Map.of("alertId", alert.getId().toString())));
        webSocketEventPublisher.sendAfterCommit(WebSocketTopics.branchAlerts(alert.getBranch().getId()), eventType, 1, response);
        if (alert.getStation() != null) {
            webSocketEventPublisher.sendAfterCommit(WebSocketTopics.station(alert.getStation().getId()), eventType, 1, response);
        }
    }
}
