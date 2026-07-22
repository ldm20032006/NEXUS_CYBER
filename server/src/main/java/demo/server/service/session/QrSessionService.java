package demo.server.service.session;

import demo.server.common.audit.AuditRecorder;
import demo.server.common.enums.AuditAction;
import demo.server.common.enums.QrLoginSessionStatus;
import demo.server.common.enums.SessionStatus;
import demo.server.common.enums.StationStatus;
import demo.server.common.enums.UserStatus;
import demo.server.common.event.DomainEventEnvelopeFactory;
import demo.server.common.event.DomainEventPublisher;
import demo.server.common.resilience.DistributedLockService;
import demo.server.common.resilience.IdempotencyDecision;
import demo.server.common.resilience.IdempotencyDecisionType;
import demo.server.common.resilience.IdempotencyService;
import demo.server.common.resilience.LockHandle;
import demo.server.common.resilience.ResilienceKeys;
import demo.server.common.security.CurrentUserProvider;
import demo.server.common.security.SecureTokenGenerator;
import demo.server.common.security.TokenHashService;
import demo.server.common.websocket.WebSocketEventPublisher;
import demo.server.common.websocket.WebSocketTopics;
import demo.server.dto.session.PlaySessionResponse;
import demo.server.dto.session.QrConfirmRequest;
import demo.server.dto.session.QrLoginSessionResponse;
import demo.server.entity.auth.AppUser;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import demo.server.entity.branch.StationCredential;
import demo.server.entity.session.PlaySession;
import demo.server.entity.session.QrLoginSession;
import demo.server.exception.BusinessRuleException;
import demo.server.exception.ConcurrencyConflictException;
import demo.server.exception.ForbiddenException;
import demo.server.exception.InvalidTransitionException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.exception.UnauthorizedException;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.branch.StationCredentialRepository;
import demo.server.repository.branch.StationRepository;
import demo.server.repository.session.PlaySessionRepository;
import demo.server.repository.session.QrLoginSessionRepository;
import demo.server.service.wallet.WalletService;
import demo.server.service.lfg.LfgLobbyService.SessionEndedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class QrSessionService {

    private static final Duration QR_TTL = Duration.ofSeconds(60);
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final QrLoginSessionRepository qrRepository;
    private final PlaySessionRepository sessionRepository;
    private final StationRepository stationRepository;
    private final StationCredentialRepository credentialRepository;
    private final AppUserRepository appUserRepository;
    private final TokenHashService tokenHashService;
    private final SecureTokenGenerator tokenGenerator;
    private final CurrentUserProvider currentUserProvider;
    private final DistributedLockService lockService;
    private final IdempotencyService idempotencyService;
    private final ResilienceKeys resilienceKeys;
    private final SessionMapper mapper;
    private final AuditRecorder auditRecorder;
    private final DomainEventPublisher domainEventPublisher;
    private final DomainEventEnvelopeFactory envelopeFactory;
    private final WebSocketEventPublisher webSocketEventPublisher;
    private final WalletService walletService;
    private final SessionBillingPolicyService billingPolicyService;

    public QrSessionService(QrLoginSessionRepository qrRepository, PlaySessionRepository sessionRepository,
                            StationRepository stationRepository, StationCredentialRepository credentialRepository,
                            AppUserRepository appUserRepository,
                            TokenHashService tokenHashService, SecureTokenGenerator tokenGenerator,
                            CurrentUserProvider currentUserProvider, DistributedLockService lockService,
                            IdempotencyService idempotencyService, ResilienceKeys resilienceKeys, SessionMapper mapper,
                            AuditRecorder auditRecorder, DomainEventPublisher domainEventPublisher,
                            DomainEventEnvelopeFactory envelopeFactory,
                            WebSocketEventPublisher webSocketEventPublisher, WalletService walletService,
                            SessionBillingPolicyService billingPolicyService) {
        this.qrRepository = qrRepository;
        this.sessionRepository = sessionRepository;
        this.stationRepository = stationRepository;
        this.credentialRepository = credentialRepository;
        this.appUserRepository = appUserRepository;
        this.tokenHashService = tokenHashService;
        this.tokenGenerator = tokenGenerator;
        this.currentUserProvider = currentUserProvider;
        this.lockService = lockService;
        this.idempotencyService = idempotencyService;
        this.resilienceKeys = resilienceKeys;
        this.mapper = mapper;
        this.auditRecorder = auditRecorder;
        this.domainEventPublisher = domainEventPublisher;
        this.envelopeFactory = envelopeFactory;
        this.webSocketEventPublisher = webSocketEventPublisher;
        this.walletService = walletService;
        this.billingPolicyService = billingPolicyService;
    }

    @Transactional
    public QrLoginSessionResponse createQr(UUID stationId, String rawSecret, String idempotencyKey) {
        Station station = authenticateStation(stationId, rawSecret);
        ensureStationAvailable(station);
        String key = idempotencyKey("create-qr", idempotencyKey);
        if (key != null) {
            Optional<QrLoginSession> existing = qrRepository.findByIdempotencyKey(key);
            if (existing.isPresent()) {
                return mapper.toQrResponse(existing.get());
            }
        }
        try (LockHandle ignored = acquire(resilienceKeys.lockQr(stationId))) {
            QrLoginSession qr = new QrLoginSession();
            qr.setStation(station);
            qr.setNonce(tokenGenerator.generate());
            qr.setExpiresAt(Instant.now().plus(QR_TTL));
            qr.setStatus(QrLoginSessionStatus.PENDING);
            qr.setQrPayload(qrPayload(station.getId(), qr.getId(), qr.getNonce(), qr.getExpiresAt()));
            qr.setIdempotencyKey(key);
            QrLoginSession saved = qrRepository.saveAndFlush(qr);
            saved.setQrPayload(qrPayload(station.getId(), saved.getId(), saved.getNonce(), saved.getExpiresAt()));
            auditRecorder.record(AuditAction.CREATE_QR_LOGIN_SESSION, "QrLoginSession", saved.getId(), null, mapper.toQrResponse(saved));
            return mapper.toQrResponse(saved);
        }
    }

    @Transactional(readOnly = true)
    public QrLoginSessionResponse getQr(UUID id) {
        return mapper.toQrResponse(qr(id));
    }

    @Transactional
    public void cancelQr(UUID id, String rawSecret) {
        QrLoginSession qr = qr(id);
        authenticateStation(qr.getStation().getId(), rawSecret);
        if (qr.getStatus() != QrLoginSessionStatus.PENDING) {
            throw new InvalidTransitionException("QR session is not pending");
        }
        qr.setStatus(QrLoginSessionStatus.CANCELLED);
        auditRecorder.record(AuditAction.CANCEL_QR_LOGIN_SESSION, "QrLoginSession", id, null, mapper.toQrResponse(qr));
    }

    @Transactional
    public PlaySessionResponse confirm(UUID qrId, QrConfirmRequest request, String idempotencyKey) {
        UUID userId = currentUserProvider.currentUserId().orElseThrow(() -> new UnauthorizedException("Authentication is required"));
        String key = idempotencyKey("confirm-qr", idempotencyKey);
        if (key != null) {
            IdempotencyDecision decision = idempotencyService.begin(key, qrId + ":" + userId + ":" + request.nonce(), Duration.ofHours(24));
            if (decision.type() == IdempotencyDecisionType.REPLAY) {
                return sessionRepository.findByIdempotencyKey(key).map(mapper::toPlaySession)
                        .orElseThrow(() -> new ConcurrencyConflictException("Idempotent result is not available"));
            }
            if (decision.type() != IdempotencyDecisionType.STARTED) {
                throw new ConcurrencyConflictException("Request with this Idempotency-Key cannot proceed");
            }
        }
        try (LockHandle qrLock = acquire(resilienceKeys.lockQr(qrId));
             LockHandle userLock = acquire(resilienceKeys.lockSession(userId))) {
            QrLoginSession qr = qr(qrId);
            Station station = qr.getStation();
            try (LockHandle stationLock = acquire(resilienceKeys.lockSession(station.getId()))) {
                PlaySession session = confirmLocked(qr, request, userId, key);
                if (key != null) {
                    idempotencyService.complete(key, 200);
                }
                return mapper.toPlaySession(session);
            }
        } catch (RuntimeException ex) {
            if (key != null) {
                idempotencyService.fail(key, 409);
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Optional<PlaySessionResponse> current() {
        UUID userId = currentUserProvider.currentUserId().orElseThrow(() -> new UnauthorizedException("Authentication is required"));
        return sessionRepository.findFirstByUser_IdAndStatusOrderByStartedAtDesc(userId, SessionStatus.ACTIVE).map(mapper::toPlaySession);
    }

    @Transactional(readOnly = true)
    public List<PlaySessionResponse> history() {
        UUID userId = currentUserProvider.currentUserId().orElseThrow(() -> new UnauthorizedException("Authentication is required"));
        return sessionRepository.findByUser_IdOrderByStartedAtDesc(userId).stream().map(mapper::toPlaySession).toList();
    }

    @Transactional
    public PlaySessionResponse end(UUID sessionId, String reason) {
        UUID userId = currentUserProvider.currentUserId().orElseThrow(() -> new UnauthorizedException("Authentication is required"));
        PlaySession session = sessionRepository.findById(sessionId).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!session.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Session is outside current user scope");
        }
        if (session.getStatus() != SessionStatus.ACTIVE && session.getStatus() != SessionStatus.PAUSED) {
            throw new InvalidTransitionException("Session cannot be ended from current status");
        }
        finishSession(session, SessionStatus.COMPLETED, reason == null ? "User ended session" : reason);
        return mapper.toPlaySession(session);
    }

    private PlaySession confirmLocked(QrLoginSession qr, QrConfirmRequest request, UUID userId, String idempotencyKey) {
        if (!qr.getNonce().equals(request.nonce())) {
            throw new BusinessRuleException("QR nonce is invalid");
        }
        if (qr.getStatus() != QrLoginSessionStatus.PENDING || qr.getConsumedAt() != null) {
            throw new InvalidTransitionException("QR session was already used");
        }
        if (qr.getExpiresAt().isBefore(Instant.now())) {
            qr.setStatus(QrLoginSessionStatus.EXPIRED);
            throw new InvalidTransitionException("QR session has expired");
        }
        AppUser user = appUserRepository.findById(userId).orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is not active");
        }
        Station station = qr.getStation();
        ensureStationAvailable(station);
        if (sessionRepository.existsByUser_IdAndStatus(userId, SessionStatus.ACTIVE)) {
            throw new BusinessRuleException("Gamer already has an active session");
        }
        if (sessionRepository.existsByStation_IdAndStatus(station.getId(), SessionStatus.ACTIVE)) {
            throw new BusinessRuleException("Station already has an active session");
        }
        BigDecimal startBalance = walletService.getOrCreateWallet(userId).getBalance();
        Branch branch = station.getBranch();
        if (branch.isPaymentEnabled() && "WALLET_REQUIRED".equalsIgnoreCase(branch.getPaymentPolicy()) && startBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Wallet balance is required to start session");
        }
        PlaySession session = new PlaySession();
        session.setUser(user);
        session.setStation(station);
        session.setQrLoginSession(qr);
        session.setStatus(SessionStatus.ACTIVE);
        session.setStartedAt(Instant.now());
        session.setStartBalance(startBalance);
        session.setEndBalance(startBalance);
        session.setIdempotencyKey(idempotencyKey);
        qr.setStatus(QrLoginSessionStatus.USED);
        qr.setConsumedAt(Instant.now());
        station.setStatus(StationStatus.OCCUPIED);
        PlaySession saved = sessionRepository.save(session);
        auditRecorder.record(AuditAction.CONFIRM_QR_LOGIN_SESSION, "QrLoginSession", qr.getId(), null, mapper.toQrResponse(qr));
        auditRecorder.record(AuditAction.START_PLAY_SESSION, "PlaySession", saved.getId(), null, mapper.toPlaySession(saved));
        publishStarted(saved);
        return saved;
    }

    private void finishSession(PlaySession session, SessionStatus status, String reason) {
        session.setStatus(status);
        session.setEndedAt(Instant.now());
        session.setEndedReason(reason);
        session.setDurationMinutes((int) ChronoUnit.MINUTES.between(session.getStartedAt(), session.getEndedAt()));
        BigDecimal finalCost = finalCost(session);
        session.setEstimatedCost(finalCost);
        session.setActualCost(finalCost);
        if (finalCost.compareTo(BigDecimal.ZERO) > 0) {
            walletService.chargeSession(session, finalCost, "play-session:" + session.getId());
        } else {
            session.setEndBalance(walletService.getOrCreateWallet(session.getUser().getId()).getBalance());
        }
        session.getStation().setStatus(StationStatus.AVAILABLE);
        auditRecorder.record(AuditAction.END_PLAY_SESSION, "PlaySession", session.getId(), null, mapper.toPlaySession(session));
        publishEnded(session);
    }

    private BigDecimal finalCost(PlaySession session) {
        return billingPolicyService.finalCostOrZero(session);
    }

    private Station authenticateStation(UUID stationId, String rawSecret) {
        if (!StringUtils.hasText(rawSecret)) {
            throw new UnauthorizedException("Station credential is required");
        }
        StationCredential credential = credentialRepository.findFirstByStation_IdAndRevokedAtIsNullOrderByIssuedAtDesc(stationId)
                .orElseThrow(() -> new UnauthorizedException("Station credential is invalid"));
        if (!tokenHashService.hash(rawSecret).equals(credential.getSecretHash())) {
            throw new UnauthorizedException("Station credential is invalid");
        }
        if (credential.getExpiresAt() != null && credential.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Station credential has expired");
        }
        credential.setLastUsedAt(Instant.now());
        return stationRepository.findById(stationId).filter(station -> !station.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Station not found"));
    }

    private void ensureStationAvailable(Station station) {
        if (station.getStatus() != StationStatus.AVAILABLE) {
            throw new BusinessRuleException("Station is not available");
        }
    }

    private QrLoginSession qr(UUID id) {
        return qrRepository.findById(id).filter(qr -> !qr.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("QR session not found"));
    }

    private LockHandle acquire(String key) {
        return lockService.tryAcquire(key, LOCK_TTL).orElseThrow(() -> new ConcurrencyConflictException("Resource is locked"));
    }

    private String idempotencyKey(String action, String rawKey) {
        return StringUtils.hasText(rawKey) ? resilienceKeys.idempotency(action, rawKey.trim()) : null;
    }

    private String qrPayload(UUID stationId, UUID qrSessionId, String nonce, Instant expiresAt) {
        return "nexus://qr-login?stationId=" + stationId
                + "&qrSessionId=" + (qrSessionId == null ? "" : qrSessionId)
                + "&nonce=" + nonce
                + "&expiresAt=" + expiresAt;
    }

    private void publishStarted(PlaySession session) {
        PlaySessionResponse response = mapper.toPlaySession(session);
        domainEventPublisher.publishAfterCommit(envelopeFactory.create("SESSION_STARTED", 1, Map.of("sessionId", session.getId().toString())));
        webSocketEventPublisher.sendAfterCommit(WebSocketTopics.station(session.getStation().getId()), "SESSION_STARTED", 1, response);
        webSocketEventPublisher.sendAfterCommit(WebSocketTopics.user(session.getUser().getId()), "SESSION_STARTED", 1, response);
    }

    private void publishEnded(PlaySession session) {
        PlaySessionResponse response = mapper.toPlaySession(session);
        domainEventPublisher.publishAfterCommit(envelopeFactory.create("SESSION_ENDED", 1, Map.of("sessionId", session.getId().toString())));
        domainEventPublisher.publishAfterCommit(new SessionEndedEvent(session.getId()));
        webSocketEventPublisher.sendAfterCommit(WebSocketTopics.station(session.getStation().getId()), "SESSION_ENDED", 1, response);
        webSocketEventPublisher.sendAfterCommit(WebSocketTopics.user(session.getUser().getId()), "SESSION_ENDED", 1, response);
    }
}
