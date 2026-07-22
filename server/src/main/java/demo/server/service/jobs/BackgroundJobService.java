package demo.server.service.jobs;

import demo.server.common.audit.AuditRecorder;
import demo.server.common.enums.AlertSeverity;
import demo.server.common.enums.AuditAction;
import demo.server.common.enums.DeviceStatus;
import demo.server.common.enums.InvitationStatus;
import demo.server.common.enums.LfgSignalStatus;
import demo.server.common.enums.NotificationDeliveryStatus;
import demo.server.common.enums.PaymentTransactionStatus;
import demo.server.common.enums.QrLoginSessionStatus;
import demo.server.common.enums.SessionStatus;
import demo.server.common.enums.StationStatus;
import demo.server.common.resilience.DistributedLockService;
import demo.server.common.resilience.LockHandle;
import demo.server.common.resilience.ResilienceKeys;
import demo.server.common.time.ClockProvider;
import demo.server.entity.iot.IotDevice;
import demo.server.entity.payment.PaymentTransaction;
import demo.server.entity.session.PlaySession;
import demo.server.payment.PaymentGatewayPort;
import demo.server.payment.PaymentReconciliationPort;
import demo.server.repository.auth.PasswordResetTokenRepository;
import demo.server.repository.auth.RefreshTokenRepository;
import demo.server.repository.branch.StationRepository;
import demo.server.repository.iot.IotDeviceRepository;
import demo.server.repository.lfg.LfgSignalRepository;
import demo.server.repository.lfg.TeamInvitationRepository;
import demo.server.repository.lobby.LobbyMessageRepository;
import demo.server.repository.notification.NotificationRepository;
import demo.server.repository.payment.PaymentTransactionRepository;
import demo.server.repository.session.PlaySessionRepository;
import demo.server.repository.session.QrLoginSessionRepository;
import demo.server.service.iot.DeviceAlertService;
import demo.server.service.notification.NotificationService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class BackgroundJobService {

    private static final Logger log = LoggerFactory.getLogger(BackgroundJobService.class);

    private final BackgroundJobProperties properties;
    private final ClockProvider clockProvider;
    private final DistributedLockService lockService;
    private final ResilienceKeys resilienceKeys;
    private final QrLoginSessionRepository qrRepository;
    private final TeamInvitationRepository invitationRepository;
    private final LfgSignalRepository lfgRepository;
    private final StationRepository stationRepository;
    private final IotDeviceRepository deviceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final NotificationRepository notificationRepository;
    private final LobbyMessageRepository lobbyMessageRepository;
    private final PlaySessionRepository sessionRepository;
    private final NotificationService notificationService;
    private final PaymentTransactionRepository paymentRepository;
    private final PaymentGatewayPort paymentGatewayPort;
    private final DeviceAlertService deviceAlertService;
    private final AuditRecorder auditRecorder;
    private final MeterRegistry meterRegistry;

    public BackgroundJobService(BackgroundJobProperties properties, ClockProvider clockProvider,
                                DistributedLockService lockService, ResilienceKeys resilienceKeys,
                                QrLoginSessionRepository qrRepository, TeamInvitationRepository invitationRepository,
                                LfgSignalRepository lfgRepository, StationRepository stationRepository,
                                IotDeviceRepository deviceRepository, RefreshTokenRepository refreshTokenRepository,
                                PasswordResetTokenRepository passwordResetTokenRepository,
                                NotificationRepository notificationRepository, LobbyMessageRepository lobbyMessageRepository,
                                PlaySessionRepository sessionRepository, NotificationService notificationService,
                                PaymentTransactionRepository paymentRepository, PaymentGatewayPort paymentGatewayPort,
                                DeviceAlertService deviceAlertService, AuditRecorder auditRecorder, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.clockProvider = clockProvider;
        this.lockService = lockService;
        this.resilienceKeys = resilienceKeys;
        this.qrRepository = qrRepository;
        this.invitationRepository = invitationRepository;
        this.lfgRepository = lfgRepository;
        this.stationRepository = stationRepository;
        this.deviceRepository = deviceRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.notificationRepository = notificationRepository;
        this.lobbyMessageRepository = lobbyMessageRepository;
        this.sessionRepository = sessionRepository;
        this.notificationService = notificationService;
        this.paymentRepository = paymentRepository;
        this.paymentGatewayPort = paymentGatewayPort;
        this.deviceAlertService = deviceAlertService;
        this.auditRecorder = auditRecorder;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelayString = "${nexus.jobs.qr-expire-interval:30000}")
    public void scheduledExpireQr() {
        runLocked("expire-qr", this::expireQr);
    }

    @Scheduled(fixedDelayString = "${nexus.jobs.invitation-expire-interval:30000}")
    public void scheduledExpireInvitations() {
        runLocked("expire-invitations", this::expireInvitations);
    }

    @Scheduled(fixedDelayString = "${nexus.jobs.lfg-expire-interval:60000}")
    public void scheduledExpireLfg() {
        runLocked("expire-lfg", this::expireLfg);
    }

    @Scheduled(fixedDelayString = "${nexus.jobs.heartbeat-interval:60000}")
    public void scheduledHeartbeatTimeouts() {
        runLocked("heartbeat-timeouts", () -> stationHeartbeatTimeouts() + deviceHeartbeatTimeouts());
    }

    @Scheduled(fixedDelayString = "${nexus.jobs.token-cleanup-interval:3600000}")
    public void scheduledTokenCleanup() {
        runLocked("token-cleanup", this::cleanupTokens);
    }

    @Scheduled(fixedDelayString = "${nexus.jobs.session-warning-interval:60000}")
    public void scheduledSessionEndingWarnings() {
        runLocked("session-ending-warning", this::sessionEndingWarnings);
    }

    @Scheduled(fixedDelayString = "${nexus.jobs.notification-retry-interval:300000}")
    public void scheduledNotificationRetry() {
        runLocked("notification-retry", () -> notificationService.retryDueDeliveries().size());
    }

    @Scheduled(fixedDelayString = "${nexus.jobs.retention-interval:86400000}")
    public void scheduledRetention() {
        runLocked("retention", this::retention);
    }

    @Scheduled(fixedDelayString = "${nexus.jobs.payment-reconciliation-interval:600000}")
    public void scheduledPaymentReconciliation() {
        runLocked("payment-reconciliation", this::paymentReconciliation);
    }

    @Transactional
    public int expireQr() {
        Instant now = clockProvider.now();
        List<UUID> ids = qrRepository.findExpiredIds(QrLoginSessionStatus.PENDING, now, page());
        int updated = ids.isEmpty() ? 0 : qrRepository.updateStatusByIds(ids, QrLoginSessionStatus.PENDING, QrLoginSessionStatus.EXPIRED);
        audit("expire-qr", updated);
        return updated;
    }

    @Transactional
    public int expireInvitations() {
        Instant now = clockProvider.now();
        List<UUID> ids = invitationRepository.findExpiredIds(InvitationStatus.PENDING, now, page());
        int updated = ids.isEmpty() ? 0 : invitationRepository.updateExpiredByIds(ids, InvitationStatus.PENDING, InvitationStatus.EXPIRED, now);
        audit("expire-invitations", updated);
        return updated;
    }

    @Transactional
    public int expireLfg() {
        Instant now = clockProvider.now();
        List<UUID> ids = lfgRepository.findExpiredIds(LfgSignalStatus.ACTIVE, now, page());
        int updated = ids.isEmpty() ? 0 : lfgRepository.updateStatusByIds(ids, LfgSignalStatus.ACTIVE, LfgSignalStatus.EXPIRED);
        audit("expire-lfg", updated);
        return updated;
    }

    @Transactional
    public int stationHeartbeatTimeouts() {
        Instant before = clockProvider.now().minus(properties.stationHeartbeatTimeout());
        List<UUID> ids = stationRepository.findHeartbeatTimedOutIds(StationStatus.OFFLINE, before, page());
        int updated = ids.isEmpty() ? 0 : stationRepository.markOfflineByIds(ids, StationStatus.OFFLINE);
        audit("station-heartbeat-timeout", updated);
        return updated;
    }

    @Transactional
    public int deviceHeartbeatTimeouts() {
        Instant before = clockProvider.now().minus(properties.deviceHeartbeatTimeout());
        List<UUID> ids = deviceRepository.findHeartbeatTimedOutIds(DeviceStatus.OFFLINE, before, page());
        int updated = ids.isEmpty() ? 0 : deviceRepository.markOfflineByIds(ids, DeviceStatus.OFFLINE);
        if (!ids.isEmpty()) {
            List<IotDevice> devices = deviceRepository.findAllById(ids);
            devices.forEach(device -> deviceAlertService.createSystemAlert(device, "HEARTBEAT_TIMEOUT",
                    "Device heartbeat timeout", "Device heartbeat timed out", AlertSeverity.HIGH, false));
        }
        audit("device-heartbeat-timeout", updated);
        return updated;
    }

    @Transactional
    public int cleanupTokens() {
        Instant before = clockProvider.now();
        int refresh = refreshTokenRepository.deleteExpiredOrRevokedBefore(before);
        int reset = passwordResetTokenRepository.deleteExpiredOrUsedBefore(before);
        int total = refresh + reset;
        audit("token-cleanup", total);
        return total;
    }

    @Transactional
    public int sessionEndingWarnings() {
        Instant before = clockProvider.now().minus(properties.sessionEndingWarningBefore());
        List<PlaySession> sessions = sessionRepository.findSessionsNeedingEndingWarning(SessionStatus.ACTIVE, before, page());
        int sent = 0;
        for (PlaySession session : sessions) {
            boolean created = notificationService.sessionEndingWarning(session.getUser().getId(), session.getId(),
                    "Session ending reminder", "Your active session may need attention soon.");
            if (created) {
                sent++;
            }
        }
        audit("session-ending-warning", sent);
        return sent;
    }

    @Transactional
    public int retention() {
        Instant now = clockProvider.now();
        int notifications = notificationRepository.softDeleteOlderThan(now.minus(properties.notificationRetention()), now);
        int chat = lobbyMessageRepository.softDeleteOlderThan(now.minus(properties.chatRetention()), now);
        audit("retention", notifications + chat);
        if (properties.auditRetention().toDays() < 365) {
            log.warn("auditRetention={} is below required minimum; audit logs are not deleted by this job", properties.auditRetention());
        }
        return notifications + chat;
    }

    @Transactional
    public int paymentReconciliation() {
        if (!(paymentGatewayPort instanceof PaymentReconciliationPort reconciliationPort)) {
            audit("payment-reconciliation-skipped", 0);
            return 0;
        }
        Instant before = clockProvider.now().minus(properties.paymentReconciliationInterval());
        List<UUID> ids = paymentRepository.findReconciliationCandidateIds(
                List.of(PaymentTransactionStatus.PENDING, PaymentTransactionStatus.PROCESSING), before, page());
        int checked = 0;
        for (UUID id : ids) {
            Optional<PaymentTransaction> transaction = paymentRepository.findById(id);
            if (transaction.isPresent()) {
                reconciliationPort.reconcile(transaction.get());
                checked++;
            }
        }
        audit("payment-reconciliation", checked);
        return checked;
    }

    private void runLocked(String jobName, Supplier<Integer> action) {
        if (!properties.enabled()) {
            return;
        }
        try (LockHandle ignored = lockService.tryAcquire(resilienceKeys.lockJob(jobName), properties.lockTtl()).orElse(null)) {
            if (ignored == null) {
                log.debug("Background job {} skipped because lock is held", jobName);
                return;
            }
            int processed = action.get();
            meterRegistry.counter("nexus.background.job.processed", "job", jobName).increment(processed);
            log.info("Background job {} completed processed={} at={}", jobName, processed, clockProvider.now());
        } catch (RuntimeException ex) {
            meterRegistry.counter("nexus.background.job.errors", "job", jobName).increment();
            log.warn("Background job {} failed at={}: {}", jobName, clockProvider.now(), ex.getMessage());
        }
    }

    private PageRequest page() {
        return PageRequest.of(0, Math.max(1, properties.batchSize()));
    }

    private void audit(String jobName, int processed) {
        auditRecorder.record(AuditAction.SYSTEM_CONFIGURATION_CHANGE, "BackgroundJob", null, null,
                Map.of("job", jobName, "processed", processed, "generatedAt", clockProvider.now().toString(), "timezone", "UTC"));
    }
}
