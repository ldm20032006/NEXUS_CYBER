package demo.server.service.iot;

import demo.server.common.audit.AuditRecorder;
import demo.server.common.enums.AlertSeverity;
import demo.server.common.enums.CommandBatchStatus;
import demo.server.common.enums.DeviceCommandStatus;
import demo.server.common.enums.DeviceCommandType;
import demo.server.common.enums.DeviceStatus;
import demo.server.common.enums.AuditAction;
import demo.server.common.enums.SessionStatus;
import demo.server.common.event.DomainEventEnvelopeFactory;
import demo.server.common.event.DomainEventPublisher;
import demo.server.common.logging.CorrelationIdFilter;
import demo.server.common.resilience.DistributedLockService;
import demo.server.common.resilience.LockHandle;
import demo.server.common.resilience.ResilienceKeys;
import demo.server.common.security.CurrentUserProvider;
import demo.server.common.security.TokenHashService;
import demo.server.common.time.ClockProvider;
import demo.server.common.websocket.WebSocketEventPublisher;
import demo.server.common.websocket.WebSocketTopics;
import demo.server.dto.iot.command.ApplyStationPreferenceResponse;
import demo.server.dto.iot.command.CommandHistoryResponse;
import demo.server.dto.iot.command.DeviceCommandAckRequest;
import demo.server.dto.iot.command.DeviceCommandPayload;
import demo.server.dto.iot.command.DeviceCommandResponse;
import demo.server.entity.auth.AppUser;
import demo.server.entity.branch.Station;
import demo.server.entity.branch.StationCredential;
import demo.server.entity.gamer.StationPreference;
import demo.server.entity.iot.CommandHistory;
import demo.server.entity.iot.DeviceCommand;
import demo.server.entity.iot.IotDevice;
import demo.server.entity.session.PlaySession;
import demo.server.exception.BusinessRuleException;
import demo.server.exception.ConcurrencyConflictException;
import demo.server.exception.ForbiddenException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.exception.UnauthorizedException;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.branch.StationCredentialRepository;
import demo.server.repository.branch.StationRepository;
import demo.server.repository.gamer.StationPreferenceRepository;
import demo.server.repository.iot.CommandHistoryRepository;
import demo.server.repository.iot.DeviceCommandRepository;
import demo.server.repository.iot.IotDeviceRepository;
import demo.server.repository.session.PlaySessionRepository;
import demo.server.service.branch.BranchScope;
import demo.server.service.iot.mqtt.MqttPublishResult;
import demo.server.service.iot.mqtt.MqttPublisher;
import demo.server.service.iot.mqtt.MqttSubscriber;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DeviceCommandService implements MqttSubscriber {

    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final Duration ACK_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_SAFE_ATTEMPTS = 3;

    private final DeviceCommandRepository commandRepository;
    private final CommandHistoryRepository historyRepository;
    private final IotDeviceRepository deviceRepository;
    private final StationRepository stationRepository;
    private final PlaySessionRepository sessionRepository;
    private final StationPreferenceRepository preferenceRepository;
    private final AppUserRepository userRepository;
    private final StationCredentialRepository credentialRepository;
    private final TokenHashService tokenHashService;
    private final CurrentUserProvider currentUserProvider;
    private final BranchScope branchScope;
    private final DistributedLockService lockService;
    private final ResilienceKeys resilienceKeys;
    private final ClockProvider clockProvider;
    private final MqttPublisher mqttPublisher;
    private final DeviceCommandMapper mapper;
    private final AuditRecorder auditRecorder;
    private final DomainEventPublisher domainEventPublisher;
    private final DomainEventEnvelopeFactory envelopeFactory;
    private final WebSocketEventPublisher webSocketEventPublisher;
    private final DeviceAlertService alertService;

    public DeviceCommandService(DeviceCommandRepository commandRepository, CommandHistoryRepository historyRepository,
                                IotDeviceRepository deviceRepository, StationRepository stationRepository,
                                PlaySessionRepository sessionRepository, StationPreferenceRepository preferenceRepository,
                                AppUserRepository userRepository, StationCredentialRepository credentialRepository,
                                TokenHashService tokenHashService, CurrentUserProvider currentUserProvider,
                                BranchScope branchScope, DistributedLockService lockService, ResilienceKeys resilienceKeys,
                                ClockProvider clockProvider, MqttPublisher mqttPublisher, DeviceCommandMapper mapper,
                                AuditRecorder auditRecorder, DomainEventPublisher domainEventPublisher,
                                DomainEventEnvelopeFactory envelopeFactory, WebSocketEventPublisher webSocketEventPublisher,
                                DeviceAlertService alertService) {
        this.commandRepository = commandRepository;
        this.historyRepository = historyRepository;
        this.deviceRepository = deviceRepository;
        this.stationRepository = stationRepository;
        this.sessionRepository = sessionRepository;
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.tokenHashService = tokenHashService;
        this.currentUserProvider = currentUserProvider;
        this.branchScope = branchScope;
        this.lockService = lockService;
        this.resilienceKeys = resilienceKeys;
        this.clockProvider = clockProvider;
        this.mqttPublisher = mqttPublisher;
        this.mapper = mapper;
        this.auditRecorder = auditRecorder;
        this.domainEventPublisher = domainEventPublisher;
        this.envelopeFactory = envelopeFactory;
        this.webSocketEventPublisher = webSocketEventPublisher;
        this.alertService = alertService;
    }

    @Transactional
    public ApplyStationPreferenceResponse applyPreference(UUID stationId, UUID requestedCorrelationId) {
        UUID userId = currentUserProvider.currentUserId().orElseThrow(() -> new UnauthorizedException("Authentication is required"));
        Station station = station(stationId);
        PlaySession session = sessionRepository.findFirstByStation_IdAndStatusOrderByStartedAtDesc(stationId, SessionStatus.ACTIVE)
                .filter(item -> item.getUser().getId().equals(userId))
                .orElseThrow(() -> new ForbiddenException("Active session at this station is required"));
        StationPreference preference = preferenceRepository.findByUser_Id(userId).orElseGet(() -> defaultPreference(user(userId)));
        List<IotDevice> devices = deviceRepository.findByBranchId(station.getBranch().getId()).stream()
                .filter(device -> !device.isDeleted())
                .filter(device -> device.getStation() != null && device.getStation().getId().equals(stationId))
                .toList();
        List<DeviceCommand> commands = new ArrayList<>();
        commands.add(createPreferenceCommand(station, session, devices, DeviceCommandType.DESK_HEIGHT_CM, preference.getDeskHeightCm(), "cm", requestedCorrelationId));
        commands.add(createPreferenceCommand(station, session, devices, DeviceCommandType.CHAIR_ANGLE_DEGREE, preference.getChairAngleDegree(), "degree", null));
        commands.add(createPreferenceCommand(station, session, devices, DeviceCommandType.RGB_COLOR, preference.getRgbColor(), null, null));
        commands.add(createPreferenceCommand(station, session, devices, DeviceCommandType.BRIGHTNESS, preference.getBrightness(), "percent", null));
        commands.add(createPreferenceCommand(station, session, devices, DeviceCommandType.MOUSE_DPI, preference.getMouseDpi(), "dpi", null));
        commands.add(createPreferenceCommand(station, session, devices, DeviceCommandType.NIGHT_MODE, preference.getNightMode(), "boolean", null));
        List<DeviceCommand> saved = commands.stream().map(this::sendOrSkip).toList();
        ApplyStationPreferenceResponse response = aggregate(stationId, session.getId(), saved);
        auditRecorder.record(AuditAction.APPLY_STATION_PREFERENCE, "Station", stationId, null,
                Map.of("stationId", stationId, "playSessionId", session.getId(), "status", response.status(),
                        "total", response.total(), "success", response.success(), "failed", response.failed(), "skipped", response.skipped()));
        return response;
    }

    @Transactional
    public DeviceCommandResponse emergencyStop(UUID deviceId) {
        IotDevice device = device(deviceId);
        branchScope.assertBranchAllowed(device.getBranch().getId());
        DeviceCommand command = create(device, null, DeviceCommandType.EMERGENCY_STOP, "STOP", null, UUID.randomUUID(), null);
        command.setEmergency(true);
        command.setDangerous(true);
        command.setMaxAttempts(1);
        DeviceCommand saved = sendOrSkip(command);
        alertService.createSystemAlert(device, "EMERGENCY_STOP", "Emergency stop issued",
                "Critical device condition triggered emergency stop", AlertSeverity.CRITICAL, true);
        return mapper.toCommand(saved);
    }

    @Transactional(readOnly = true)
    public Page<DeviceCommandResponse> history(UUID stationId, int page, int size) {
        Station station = station(stationId);
        branchScope.assertBranchAllowed(station.getBranch().getId());
        return commandRepository.findByStationIdOrderByCreatedAtDesc(stationId, PageRequest.of(page, Math.min(size, 100))).map(mapper::toCommand);
    }

    @Transactional(readOnly = true)
    public List<CommandHistoryResponse> commandHistory(UUID commandId) {
        DeviceCommand command = commandRepository.findById(commandId)
                .orElseThrow(() -> new ResourceNotFoundException("Command not found"));
        branchScope.assertBranchAllowed(command.getBranch().getId());
        return historyRepository.findByCommandIdOrderByCreatedAtAsc(commandId).stream().map(mapper::toHistory).toList();
    }

    @Override
    @Transactional
    public void handleAck(DeviceCommandAckRequest request) {
        handleAck(request, null);
    }

    @Transactional
    public DeviceCommandResponse handleAck(DeviceCommandAckRequest request, String rawStationSecret) {
        validateStationCredential(request.stationId(), rawStationSecret);
        try (LockHandle ignored = lockService.tryAcquire(resilienceKeys.lockDeviceCommandCorrelation(request.correlationId()), LOCK_TTL)
                .orElseThrow(() -> new ConcurrencyConflictException("Command correlation is locked"))) {
            DeviceCommand command = commandRepository.findWithLockByCorrelationId(request.correlationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Command not found"));
            validateAckScope(command, request);
            if (command.getStatus() == DeviceCommandStatus.SUCCESS || command.getStatus() == DeviceCommandStatus.FAILED) {
                return mapper.toCommand(command);
            }
            DeviceCommandStatus from = command.getStatus();
            command.setStatus(request.success() ? DeviceCommandStatus.SUCCESS : DeviceCommandStatus.FAILED);
            command.setAcknowledgedAt(clockProvider.now());
            command.setResultMessage(safeNote(request.message()));
            appendHistory(command, from, command.getStatus(), "ACK", command.getResultMessage(), null);
            auditRecorder.record(AuditAction.DEVICE_COMMAND_ACK, "DeviceCommand", command.getId(), null, mapper.toCommand(command));
            publish(command, "DEVICE_COMMAND_ACK");
            return mapper.toCommand(command);
        }
    }

    @Transactional
    public List<DeviceCommandResponse> markTimeouts() {
        Instant cutoff = clockProvider.now().minus(ACK_TIMEOUT);
        return commandRepository.findByStatusAndSentAtBefore(DeviceCommandStatus.SENT, cutoff).stream()
                .map(this::timeoutOrRetry)
                .map(mapper::toCommand)
                .toList();
    }

    private DeviceCommand timeoutOrRetry(DeviceCommand command) {
        if (!command.isDangerous() && command.getAttemptCount() < command.getMaxAttempts()) {
            return sendOrSkip(command);
        }
        DeviceCommandStatus from = command.getStatus();
        command.setStatus(DeviceCommandStatus.TIMEOUT);
        command.setTimedOutAt(clockProvider.now());
        command.setResultMessage("MQTT ACK timeout");
        appendHistory(command, from, DeviceCommandStatus.TIMEOUT, "TIMEOUT", command.getResultMessage(), null);
        publish(command, "DEVICE_COMMAND_TIMEOUT");
        return command;
    }

    private DeviceCommand createPreferenceCommand(Station station, PlaySession session, List<IotDevice> devices,
                                                  DeviceCommandType type, Object value, String unit, UUID requestedCorrelationId) {
        IotDevice device = findDevice(devices, type);
        if (device == null || value == null) {
            DeviceCommand skipped = create(station, session, type, value, unit, UUID.randomUUID());
            skipped.setStatus(DeviceCommandStatus.SKIPPED);
            skipped.setResultMessage("Compatible device or preference value is missing");
            appendHistory(skipped, null, DeviceCommandStatus.SKIPPED, "SKIP", skipped.getResultMessage(), null);
            return commandRepository.save(skipped);
        }
        validateSafety(type, value, device);
        UUID correlationId = requestedCorrelationId != null ? requestedCorrelationId : UUID.randomUUID();
        return commandRepository.findByCorrelationId(correlationId).orElseGet(() -> create(device, session, type, value.toString(), unit, correlationId, user(session.getUser().getId())));
    }

    private DeviceCommand create(Station station, PlaySession session, DeviceCommandType type, Object value, String unit, UUID correlationId) {
        IotDevice placeholder = deviceRepository.findByBranchId(station.getBranch().getId()).stream()
                .filter(device -> device.getStation() != null && device.getStation().getId().equals(station.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No IoT device found for station"));
        return create(placeholder, session, type, value == null ? "" : value.toString(), unit, correlationId, session == null ? null : session.getUser());
    }

    private DeviceCommand create(IotDevice device, PlaySession session, DeviceCommandType type, String value, String unit, UUID correlationId, AppUser user) {
        DeviceCommand command = new DeviceCommand();
        command.setBranch(device.getBranch());
        command.setStation(device.getStation());
        command.setDevice(device);
        command.setUser(user);
        command.setPlaySession(session);
        command.setCorrelationId(correlationId);
        command.setCommandType(type);
        command.setCommandValue(value);
        command.setUnit(unit);
        command.setDangerous(isDangerous(type));
        command.setMaxAttempts(isDangerous(type) ? 1 : MAX_SAFE_ATTEMPTS);
        command.setMqttTopic(topic(device));
        return commandRepository.save(command);
    }

    private DeviceCommand sendOrSkip(DeviceCommand command) {
        if (command.getStatus() == DeviceCommandStatus.SKIPPED || command.getStatus() == DeviceCommandStatus.SUCCESS) {
            return command;
        }
        if (command.getDevice().getStatus() == DeviceStatus.DISABLED || command.getDevice().getStatus() == DeviceStatus.OFFLINE) {
            DeviceCommandStatus from = command.getStatus();
            command.setStatus(DeviceCommandStatus.SKIPPED);
            command.setResultMessage("Device is not available");
            appendHistory(command, from, DeviceCommandStatus.SKIPPED, "SKIP", command.getResultMessage(), null);
            publish(command, "DEVICE_COMMAND_SKIPPED");
            return command;
        }
        if (command.getDevice().isMechanicalCommandLocked() && command.getCommandType() != DeviceCommandType.EMERGENCY_STOP) {
            DeviceCommandStatus from = command.getStatus();
            command.setStatus(DeviceCommandStatus.SKIPPED);
            command.setResultMessage("Mechanical command locked by critical alert");
            appendHistory(command, from, DeviceCommandStatus.SKIPPED, "SAFETY_SKIP", command.getResultMessage(), null);
            publish(command, "DEVICE_COMMAND_SKIPPED");
            return command;
        }
        DeviceCommandStatus from = command.getStatus();
        command.setAttemptCount(command.getAttemptCount() + 1);
        command.setStatus(DeviceCommandStatus.SENT);
        command.setSentAt(clockProvider.now());
        appendHistory(command, from, DeviceCommandStatus.SENT, "PUBLISH", "MQTT over TLS is required outside mock adapter", actorOrNull());
        MqttPublishResult result = mqttPublisher.publish(command.getMqttTopic(), payload(command));
        if (!result.accepted()) {
            command.setStatus(DeviceCommandStatus.FAILED);
            command.setResultMessage(result.message());
            appendHistory(command, DeviceCommandStatus.SENT, DeviceCommandStatus.FAILED, "PUBLISH_FAILED", result.message(), actorOrNull());
        } else if (result.acknowledged()) {
            command.setStatus(result.success() ? DeviceCommandStatus.SUCCESS : DeviceCommandStatus.FAILED);
            command.setAcknowledgedAt(clockProvider.now());
            command.setResultMessage(result.message());
            appendHistory(command, DeviceCommandStatus.SENT, command.getStatus(), "MOCK_ACK", result.message(), actorOrNull());
        }
        auditRecorder.record(AuditAction.DEVICE_COMMAND, "DeviceCommand", command.getId(), null, mapper.toCommand(command));
        publish(command, "DEVICE_COMMAND_PROGRESS");
        return command;
    }

    private IotDevice findDevice(List<IotDevice> devices, DeviceCommandType type) {
        return devices.stream().filter(device -> switch (type) {
            case DESK_HEIGHT_CM -> device.getDeviceType().name().contains("DESK");
            case CHAIR_ANGLE_DEGREE -> device.getDeviceType().name().contains("CHAIR");
            case RGB_COLOR, BRIGHTNESS, NIGHT_MODE -> device.getDeviceType().name().contains("RGB") || device.getDeviceType().name().contains("LIGHT");
            case MOUSE_DPI -> device.getDeviceType().name().contains("MOUSE");
            case EMERGENCY_STOP -> true;
        }).findFirst().orElse(null);
    }

    private void validateSafety(DeviceCommandType type, Object value, IotDevice device) {
        if (device.getBranch() == null || device.getStation() == null || !device.getStation().getBranch().getId().equals(device.getBranch().getId())) {
            throw new ForbiddenException("Device branch/station scope is invalid");
        }
        String text = value.toString();
        switch (type) {
            case DESK_HEIGHT_CM -> requireRange(Integer.parseInt(text), 60, 120, "Desk height out of safe range");
            case CHAIR_ANGLE_DEGREE -> requireRange(Integer.parseInt(text), 90, 145, "Chair angle out of safe range");
            case BRIGHTNESS -> requireRange(Integer.parseInt(text), 0, 100, "Brightness out of safe range");
            case MOUSE_DPI -> requireRange(Integer.parseInt(text), 400, 6400, "Mouse DPI out of safe range");
            case RGB_COLOR -> {
                if (!text.matches("^#[0-9A-Fa-f]{6}$")) {
                    throw new BusinessRuleException("RGB color must be #RRGGBB");
                }
            }
            case NIGHT_MODE, EMERGENCY_STOP -> {
            }
        }
    }

    private void requireRange(int value, int min, int max, String message) {
        if (value < min || value > max) {
            throw new BusinessRuleException(message);
        }
    }

    private void validateAckScope(DeviceCommand command, DeviceCommandAckRequest request) {
        if (!command.getBranch().getId().equals(request.branchId())
                || !command.getStation().getId().equals(request.stationId())
                || !command.getDevice().getId().equals(request.deviceId())) {
            throw new ForbiddenException("ACK topic payload is outside command scope");
        }
    }

    private void validateStationCredential(UUID stationId, String rawSecret) {
        if (!StringUtils.hasText(rawSecret)) {
            throw new UnauthorizedException("MQTT Gateway credential is required");
        }
        StationCredential credential = credentialRepository.findFirstByStation_IdAndRevokedAtIsNullOrderByIssuedAtDesc(stationId)
                .orElseThrow(() -> new UnauthorizedException("MQTT Gateway credential is invalid"));
        if (!tokenHashService.hash(rawSecret).equals(credential.getSecretHash())) {
            throw new UnauthorizedException("MQTT Gateway credential is invalid");
        }
        if (credential.getExpiresAt() != null && credential.getExpiresAt().isBefore(clockProvider.now())) {
            throw new UnauthorizedException("MQTT Gateway credential has expired");
        }
        credential.setLastUsedAt(clockProvider.now());
    }

    private ApplyStationPreferenceResponse aggregate(UUID stationId, UUID sessionId, List<DeviceCommand> commands) {
        int success = (int) commands.stream().filter(command -> command.getStatus() == DeviceCommandStatus.SUCCESS).count();
        int skipped = (int) commands.stream().filter(command -> command.getStatus() == DeviceCommandStatus.SKIPPED).count();
        int failed = (int) commands.stream().filter(command -> command.getStatus() == DeviceCommandStatus.FAILED || command.getStatus() == DeviceCommandStatus.TIMEOUT).count();
        CommandBatchStatus status;
        if (success == commands.size()) {
            status = CommandBatchStatus.SUCCESS;
        } else if (success == 0 && skipped == commands.size()) {
            status = CommandBatchStatus.SKIPPED;
        } else if (success > 0) {
            status = CommandBatchStatus.PARTIAL_SUCCESS;
        } else {
            status = CommandBatchStatus.FAILED;
        }
        return new ApplyStationPreferenceResponse(stationId, sessionId, status, commands.size(), success, failed, skipped,
                commands.stream().map(mapper::toCommand).toList());
    }

    private void appendHistory(DeviceCommand command, DeviceCommandStatus from, DeviceCommandStatus to, String action, String note, AppUser actor) {
        CommandHistory history = new CommandHistory();
        history.setCommand(command);
        history.setActor(actor);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setAction(action);
        history.setNote(note);
        historyRepository.save(history);
    }

    private DeviceCommandPayload payload(DeviceCommand command) {
        return new DeviceCommandPayload(command.getId(), command.getCorrelationId(), command.getCommandType(),
                command.getCommandValue(), command.getUnit(), clockProvider.now());
    }

    private void publish(DeviceCommand command, String eventType) {
        DeviceCommandResponse response = mapper.toCommand(command);
        domainEventPublisher.publishAfterCommit(envelopeFactory.create(eventType, 1, Map.of("commandId", command.getId().toString())));
        webSocketEventPublisher.sendAfterCommit(WebSocketTopics.station(command.getStation().getId()), eventType, 1, response);
        webSocketEventPublisher.sendAfterCommit(WebSocketTopics.branchAlerts(command.getBranch().getId()), eventType, 1, response);
        if (command.getUser() != null) {
            webSocketEventPublisher.sendAfterCommit(WebSocketTopics.user(command.getUser().getId()), eventType, 1, response);
        }
    }

    private String topic(IotDevice device) {
        return "nexus/%s/%s/%s/command".formatted(device.getBranch().getId(), device.getStation().getId(), device.getId());
    }

    private boolean isDangerous(DeviceCommandType type) {
        return type == DeviceCommandType.DESK_HEIGHT_CM || type == DeviceCommandType.CHAIR_ANGLE_DEGREE || type == DeviceCommandType.EMERGENCY_STOP;
    }

    private StationPreference defaultPreference(AppUser user) {
        StationPreference preference = new StationPreference();
        preference.setUser(user);
        preference.setDeskHeightCm(75);
        preference.setChairAngleDegree(110);
        preference.setRgbColor("#FFFFFF");
        preference.setBrightness(70);
        preference.setMouseDpi(1600);
        preference.setNightMode(Boolean.FALSE);
        return preferenceRepository.save(preference);
    }

    private Station station(UUID id) {
        return stationRepository.findById(id).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Station not found"));
    }

    private IotDevice device(UUID id) {
        return deviceRepository.findById(id).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
    }

    private AppUser user(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AppUser actorOrNull() {
        return currentUserProvider.currentUserId().flatMap(userRepository::findById).orElse(null);
    }

    private String safeNote(String value) {
        return value == null ? null : value.substring(0, Math.min(value.length(), 1000));
    }
}
