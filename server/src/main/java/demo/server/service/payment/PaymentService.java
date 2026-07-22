package demo.server.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.audit.AuditRecorder;
import demo.server.common.enums.AuditAction;
import demo.server.common.enums.PaymentTransactionStatus;
import demo.server.common.resilience.DistributedLockService;
import demo.server.common.resilience.IdempotencyDecision;
import demo.server.common.resilience.IdempotencyDecisionType;
import demo.server.common.resilience.IdempotencyService;
import demo.server.common.resilience.LockHandle;
import demo.server.common.resilience.ResilienceKeys;
import demo.server.common.security.CurrentUserProvider;
import demo.server.dto.payment.PaymentWebhookResponse;
import demo.server.dto.payment.TopUpResponse;
import demo.server.entity.auth.AppUser;
import demo.server.entity.payment.PaymentTransaction;
import demo.server.entity.wallet.WalletTransaction;
import demo.server.exception.BusinessRuleException;
import demo.server.exception.ConcurrencyConflictException;
import demo.server.exception.ExternalServiceException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.exception.UnauthorizedException;
import demo.server.payment.PaymentGatewayPort;
import demo.server.payment.PaymentGatewayTopUp;
import demo.server.payment.PaymentProperties;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.payment.PaymentTransactionRepository;
import demo.server.service.wallet.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final String CURRENCY = "VND";

    private final PaymentTransactionRepository paymentRepository;
    private final AppUserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PaymentGatewayPort gatewayPort;
    private final PaymentProperties properties;
    private final PaymentMapper mapper;
    private final IdempotencyService idempotencyService;
    private final DistributedLockService lockService;
    private final ResilienceKeys resilienceKeys;
    private final WalletService walletService;
    private final ObjectMapper objectMapper;
    private final AuditRecorder auditRecorder;

    public PaymentService(PaymentTransactionRepository paymentRepository, AppUserRepository userRepository,
                          CurrentUserProvider currentUserProvider, PaymentGatewayPort gatewayPort,
                          PaymentProperties properties, PaymentMapper mapper, IdempotencyService idempotencyService,
                          DistributedLockService lockService, ResilienceKeys resilienceKeys, WalletService walletService,
                          ObjectMapper objectMapper, AuditRecorder auditRecorder) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.gatewayPort = gatewayPort;
        this.properties = properties;
        this.mapper = mapper;
        this.idempotencyService = idempotencyService;
        this.lockService = lockService;
        this.resilienceKeys = resilienceKeys;
        this.walletService = walletService;
        this.objectMapper = objectMapper;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public TopUpResponse startTopUp(BigDecimal amount, String idempotencyKey) {
        UUID userId = currentUserProvider.currentUserId().orElseThrow(() -> new UnauthorizedException("Authentication is required"));
        BigDecimal normalized = normalize(amount);
        String key = idempotencyKey("topup", idempotencyKey);
        if (key != null) {
            IdempotencyDecision decision = idempotencyService.begin(key, userId + ":" + normalized + ":" + CURRENCY, IDEMPOTENCY_TTL);
            if (decision.type() == IdempotencyDecisionType.REPLAY) {
                return paymentRepository.findByIdempotencyKey(key)
                        .map(transaction -> mapper.toTopUp(transaction, gatewayPort.adapterMode()))
                        .orElseThrow(() -> new ConcurrencyConflictException("Idempotent payment result is not available"));
            }
            if (decision.type() != IdempotencyDecisionType.STARTED) {
                throw new ConcurrencyConflictException("Request with this Idempotency-Key cannot proceed");
            }
        }
        try {
            AppUser user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
            PaymentGatewayTopUp gatewayTopUp = gatewayPort.startTopUp(userId, normalized, CURRENCY);
            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setUser(user);
            transaction.setProvider(gatewayTopUp.provider());
            transaction.setProviderTransactionId(gatewayTopUp.providerTransactionId());
            transaction.setStatus(PaymentTransactionStatus.PENDING);
            transaction.setAmount(normalized);
            transaction.setCurrency(CURRENCY);
            transaction.setCheckoutUrl(gatewayTopUp.checkoutUrl());
            transaction.setRequestedAt(Instant.now());
            transaction.setIdempotencyKey(key);
            PaymentTransaction saved = paymentRepository.save(transaction);
            auditRecorder.record(AuditAction.CREATE_PAYMENT_TRANSACTION, "PaymentTransaction", saved.getId(), null, mapper.toTransaction(saved));
            if (key != null) {
                idempotencyService.complete(key, 200);
            }
            return mapper.toTopUp(saved, gatewayPort.adapterMode());
        } catch (RuntimeException ex) {
            if (key != null) {
                idempotencyService.fail(key, 409);
            }
            throw ex;
        }
    }

    @Transactional
    public PaymentWebhookResponse handleWebhook(String rawBody, String timestampHeader, String signatureHeader) {
        verifyWebhook(rawBody, timestampHeader, signatureHeader);
        JsonNode payload = parse(rawBody);
        String providerTransactionId = requiredText(payload, "providerTransactionId");
        PaymentTransactionStatus status = PaymentTransactionStatus.valueOf(requiredText(payload, "status"));
        BigDecimal amount = normalize(new BigDecimal(requiredText(payload, "amount")));
        String currency = requiredText(payload, "currency");
        String fingerprint = providerTransactionId + ":" + status + ":" + amount + ":" + currency + ":" + sha256(rawBody);
        String replayKey = idempotencyKey("webhook", providerTransactionId + ":" + status);
        IdempotencyDecision decision = idempotencyService.begin(replayKey, fingerprint, IDEMPOTENCY_TTL);
        if (decision.type() == IdempotencyDecisionType.FINGERPRINT_MISMATCH) {
            throw new ConcurrencyConflictException("Payment webhook fingerprint mismatch");
        }
        if (decision.type() == IdempotencyDecisionType.IN_PROGRESS) {
            throw new ConcurrencyConflictException("Payment webhook is already processing");
        }

        try (LockHandle ignored = lockService.tryAcquire(resilienceKeys.lockPaymentCallback(gatewayPort.providerName(), providerTransactionId), LOCK_TTL)
                .orElseThrow(() -> new ConcurrencyConflictException("Payment callback is locked"))) {
            PaymentTransaction transaction = paymentRepository.findByProviderTransactionForUpdate(gatewayPort.providerName(), providerTransactionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found"));
            validateWebhookMatches(transaction, amount, currency);
            boolean credited = applyStatus(transaction, status, replayKey);
            auditRecorder.record(AuditAction.PAYMENT_WEBHOOK, "PaymentTransaction", transaction.getId(), null, mapper.toTransaction(transaction));
            idempotencyService.complete(replayKey, 200);
            return new PaymentWebhookResponse(transaction.getId(), transaction.getProviderTransactionId(), transaction.getStatus(), credited);
        } catch (RuntimeException ex) {
            idempotencyService.fail(replayKey, 409);
            throw ex;
        }
    }

    private boolean applyStatus(PaymentTransaction transaction, PaymentTransactionStatus status, String replayKey) {
        if (transaction.getStatus() == PaymentTransactionStatus.SUCCEEDED) {
            return false;
        }
        if (status == PaymentTransactionStatus.SUCCEEDED) {
            WalletTransaction walletTransaction = walletService.creditTopUp(transaction.getUser().getId(),
                    transaction.getAmount(), transaction.getId(), replayKey);
            transaction.setStatus(PaymentTransactionStatus.SUCCEEDED);
            transaction.setProcessedAt(Instant.now());
            transaction.setWalletTransaction(walletTransaction);
            return true;
        }
        if (status == PaymentTransactionStatus.FAILED || status == PaymentTransactionStatus.CANCELLED
                || status == PaymentTransactionStatus.REFUNDED || status == PaymentTransactionStatus.PROCESSING) {
            transaction.setStatus(status);
            transaction.setProcessedAt(Instant.now());
            return false;
        }
        throw new BusinessRuleException("Unsupported payment status");
    }

    private void validateWebhookMatches(PaymentTransaction transaction, BigDecimal amount, String currency) {
        if (transaction.getAmount().compareTo(amount) != 0 || !transaction.getCurrency().equals(currency)) {
            throw new BusinessRuleException("Payment webhook amount or currency mismatch");
        }
    }

    private void verifyWebhook(String rawBody, String timestampHeader, String signatureHeader) {
        if (!StringUtils.hasText(properties.webhookSecret())) {
            throw new ExternalServiceException("Payment webhook secret is not configured");
        }
        Instant timestamp = parseTimestamp(timestampHeader);
        Instant now = Instant.now();
        if (timestamp.isBefore(now.minus(properties.webhookReplayWindow()))
                || timestamp.isAfter(now.plus(Duration.ofMinutes(1)))) {
            throw new UnauthorizedException("Payment webhook timestamp is outside replay window");
        }
        String expected = hmacSha256Hex(properties.webhookSecret(), timestampHeader + "." + rawBody);
        if (!constantTimeEquals(expected, signatureHeader)) {
            throw new UnauthorizedException("Payment webhook signature is invalid");
        }
    }

    private Instant parseTimestamp(String value) {
        if (!StringUtils.hasText(value)) {
            throw new UnauthorizedException("Payment webhook timestamp is required");
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return Instant.ofEpochSecond(Long.parseLong(value));
            } catch (NumberFormatException ex) {
                throw new UnauthorizedException("Payment webhook timestamp is invalid");
            }
        }
    }

    private JsonNode parse(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception ex) {
            throw new BusinessRuleException("Payment webhook payload is invalid");
        }
    }

    private String requiredText(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || value.asText().isBlank()) {
            throw new BusinessRuleException("Payment webhook field is required: " + field);
        }
        return value.asText();
    }

    private String hmacSha256Hex(String secret, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ExternalServiceException("Payment signature verification failed", ex);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (!StringUtils.hasText(actual)) {
            return false;
        }
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ExternalServiceException("Payment webhook fingerprint failed", ex);
        }
    }

    private String idempotencyKey(String action, String rawKey) {
        return StringUtils.hasText(rawKey) ? resilienceKeys.idempotency("payment-" + action, rawKey.trim()) : null;
    }

    private BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            throw new BusinessRuleException("Amount is required");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
