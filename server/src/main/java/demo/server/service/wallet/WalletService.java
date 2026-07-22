package demo.server.service.wallet;

import demo.server.common.audit.AuditRecorder;
import demo.server.common.enums.AuditAction;
import demo.server.common.enums.TransactionType;
import demo.server.common.resilience.DistributedLockService;
import demo.server.common.resilience.IdempotencyDecision;
import demo.server.common.resilience.IdempotencyDecisionType;
import demo.server.common.resilience.IdempotencyService;
import demo.server.common.resilience.LockHandle;
import demo.server.common.resilience.ResilienceKeys;
import demo.server.common.security.CurrentUserProvider;
import demo.server.dto.wallet.WalletResponse;
import demo.server.dto.wallet.WalletTransactionResponse;
import demo.server.entity.auth.AppUser;
import demo.server.entity.session.PlaySession;
import demo.server.entity.wallet.Wallet;
import demo.server.entity.wallet.WalletTransaction;
import demo.server.exception.BusinessRuleException;
import demo.server.exception.ConcurrencyConflictException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.exception.UnauthorizedException;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.wallet.WalletRepository;
import demo.server.repository.wallet.WalletTransactionRepository;
import demo.server.service.branch.BranchScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class WalletService {

    public static final String REF_PLAY_SESSION = "PLAY_SESSION";
    public static final String REF_PAYMENT_TRANSACTION = "PAYMENT_TRANSACTION";
    public static final String REF_FOOD_ORDER = "FOOD_ORDER";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final AppUserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final DistributedLockService lockService;
    private final IdempotencyService idempotencyService;
    private final ResilienceKeys resilienceKeys;
    private final WalletMapper mapper;
    private final AuditRecorder auditRecorder;
    private final BranchScope branchScope;

    public WalletService(WalletRepository walletRepository, WalletTransactionRepository transactionRepository,
                         AppUserRepository userRepository, CurrentUserProvider currentUserProvider,
                         DistributedLockService lockService, IdempotencyService idempotencyService,
                         ResilienceKeys resilienceKeys, WalletMapper mapper, AuditRecorder auditRecorder,
                         BranchScope branchScope) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.lockService = lockService;
        this.idempotencyService = idempotencyService;
        this.resilienceKeys = resilienceKeys;
        this.mapper = mapper;
        this.auditRecorder = auditRecorder;
        this.branchScope = branchScope;
    }

    @Transactional
    public WalletResponse currentWallet() {
        UUID userId = currentUserProvider.currentUserId().orElseThrow(() -> new UnauthorizedException("Authentication is required"));
        return mapper.toWallet(getOrCreateWallet(userId));
    }

    @Transactional
    public List<WalletTransactionResponse> currentTransactions() {
        UUID userId = currentUserProvider.currentUserId().orElseThrow(() -> new UnauthorizedException("Authentication is required"));
        Wallet wallet = getOrCreateWallet(userId);
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId()).stream()
                .map(mapper::toTransaction)
                .toList();
    }

    @Transactional
    public WalletTransactionResponse adminAdjustment(UUID userId, BigDecimal amount, String reason, String idempotencyKey) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessRuleException("Adjustment reason is required");
        }
        enforceAdminScope(userId);
        WalletTransaction transaction = mutate(userId, normalize(amount), TransactionType.ADMIN_ADJUSTMENT,
                "ADMIN_ADJUSTMENT", userId.toString(), null, reason, idempotencyKey);
        auditRecorder.record(AuditAction.WALLET_ADJUSTMENT, "Wallet", transaction.getWallet().getId(), null, mapper.toTransaction(transaction));
        return mapper.toTransaction(transaction);
    }

    @Transactional
    public WalletTransactionResponse refund(UUID originalTransactionId, String reason, String idempotencyKey) {
        WalletTransaction original = transactionRepository.findById(originalTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Original wallet transaction not found"));
        enforceAdminScope(original.getUser().getId());
        if (original.getType() == TransactionType.REFUND) {
            throw new BusinessRuleException("Refund transaction cannot be refunded");
        }
        if (original.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
            throw new BusinessRuleException("Only debit transactions can be refunded");
        }
        if (transactionRepository.existsByOriginalTransaction_Id(originalTransactionId)) {
            throw new BusinessRuleException("Original transaction was already refunded");
        }
        BigDecimal refundAmount = original.getAmount().abs();
        WalletTransaction refund = mutate(original.getUser().getId(), refundAmount, TransactionType.REFUND,
                original.getReferenceType(), original.getReferenceId(), original, reason, idempotencyKey);
        auditRecorder.record(AuditAction.WALLET_REFUND, "WalletTransaction", refund.getId(), mapper.toTransaction(original), mapper.toTransaction(refund));
        return mapper.toTransaction(refund);
    }

    @Transactional
    public WalletTransactionResponse chargeSession(PlaySession session, BigDecimal amount, String idempotencyKey) {
        BigDecimal normalized = normalize(amount);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            Wallet wallet = getOrCreateWallet(session.getUser().getId());
            session.setEndBalance(wallet.getBalance());
            return null;
        }
        String referenceId = session.getId().toString();
        return transactionRepository.findFirstByReferenceTypeAndReferenceIdAndType(REF_PLAY_SESSION, referenceId, TransactionType.SESSION_CHARGE)
                .map(mapper::toTransaction)
                .orElseGet(() -> {
                    WalletTransaction transaction = mutate(session.getUser().getId(), normalized.negate(), TransactionType.SESSION_CHARGE,
                            REF_PLAY_SESSION, referenceId, null, "Session charge", idempotencyKey);
                    session.setActualCost(normalized);
                    session.setEndBalance(transaction.getBalanceAfter());
                    return mapper.toTransaction(transaction);
                });
    }

    @Transactional
    public WalletTransaction creditTopUp(UUID userId, BigDecimal amount, UUID paymentTransactionId, String idempotencyKey) {
        BigDecimal normalized = normalize(amount);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Top-up amount must be positive");
        }
        return transactionRepository.findFirstByReferenceTypeAndReferenceIdAndType(
                        REF_PAYMENT_TRANSACTION, paymentTransactionId.toString(), TransactionType.TOP_UP)
                .orElseGet(() -> mutate(userId, normalized, TransactionType.TOP_UP,
                        REF_PAYMENT_TRANSACTION, paymentTransactionId.toString(), null,
                        "Payment top-up", idempotencyKey));
    }

    @Transactional
    public WalletTransaction payOrder(UUID userId, UUID orderId, BigDecimal amount, String idempotencyKey) {
        BigDecimal normalized = normalize(amount);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Order payment amount must be positive");
        }
        return transactionRepository.findFirstByReferenceTypeAndReferenceIdAndType(
                        REF_FOOD_ORDER, orderId.toString(), TransactionType.ORDER_PAYMENT)
                .orElseGet(() -> mutate(userId, normalized.negate(), TransactionType.ORDER_PAYMENT,
                        REF_FOOD_ORDER, orderId.toString(), null, "Food order payment", idempotencyKey));
    }

    @Transactional
    public WalletTransaction refundOrder(UUID userId, UUID orderId, WalletTransaction originalTransaction, String reason,
                                         String idempotencyKey) {
        if (originalTransaction == null) {
            return null;
        }
        if (transactionRepository.existsByOriginalTransaction_Id(originalTransaction.getId())) {
            return transactionRepository.findFirstByReferenceTypeAndReferenceIdAndType(
                    REF_FOOD_ORDER, orderId.toString(), TransactionType.REFUND).orElse(null);
        }
        return mutate(userId, originalTransaction.getAmount().abs(), TransactionType.REFUND,
                REF_FOOD_ORDER, orderId.toString(), originalTransaction, reason, idempotencyKey);
    }

    @Transactional
    public Wallet getOrCreateWallet(UUID userId) {
        return walletRepository.findByUser_Id(userId).orElseGet(() -> {
            AppUser user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
            try (LockHandle ignored = acquire(userId)) {
                return walletRepository.findByUser_Id(userId).orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setUser(user);
                    wallet.setBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                    wallet.setCurrency("VND");
                    return walletRepository.saveAndFlush(wallet);
                });
            }
        });
    }

    private WalletTransaction mutate(UUID userId, BigDecimal amount, TransactionType type, String referenceType,
                                     String referenceId, WalletTransaction originalTransaction, String description,
                                     String rawIdempotencyKey) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleException("Wallet mutation amount must be non-zero");
        }
        String key = idempotencyKey(type.name(), rawIdempotencyKey);
        if (key != null) {
            IdempotencyDecision decision = idempotencyService.begin(key, userId + ":" + type + ":" + amount + ":" + referenceId, IDEMPOTENCY_TTL);
            if (decision.type() == IdempotencyDecisionType.REPLAY) {
                return transactionRepository.findByIdempotencyKey(key)
                        .orElseThrow(() -> new ConcurrencyConflictException("Idempotent wallet result is not available"));
            }
            if (decision.type() != IdempotencyDecisionType.STARTED) {
                throw new ConcurrencyConflictException("Request with this Idempotency-Key cannot proceed");
            }
        }
        getOrCreateWallet(userId);
        try (LockHandle ignored = acquire(userId)) {
            Wallet wallet = lockedWallet(userId);
            BigDecimal before = wallet.getBalance();
            BigDecimal after = before.add(amount).setScale(2, RoundingMode.HALF_UP);
            if (after.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException("Wallet balance cannot be negative");
            }
            wallet.setBalance(after);
            WalletTransaction transaction = new WalletTransaction();
            transaction.setWallet(wallet);
            transaction.setUser(wallet.getUser());
            transaction.setType(type);
            transaction.setAmount(amount);
            transaction.setCurrency(wallet.getCurrency());
            transaction.setBalanceBefore(before);
            transaction.setBalanceAfter(after);
            transaction.setReferenceType(referenceType);
            transaction.setReferenceId(referenceId);
            transaction.setOriginalTransaction(originalTransaction);
            transaction.setIdempotencyKey(key);
            transaction.setDescription(description);
            WalletTransaction saved = transactionRepository.save(transaction);
            if (key != null) {
                idempotencyService.complete(key, 200);
            }
            return saved;
        } catch (RuntimeException ex) {
            if (key != null) {
                idempotencyService.fail(key, 409);
            }
            throw ex;
        }
    }

    private Wallet lockedWallet(UUID userId) {
        return walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    private LockHandle acquire(UUID userId) {
        return lockService.tryAcquire(resilienceKeys.lockWallet(userId), LOCK_TTL)
                .orElseThrow(() -> new ConcurrencyConflictException("Wallet is locked"));
    }

    private String idempotencyKey(String action, String rawKey) {
        return StringUtils.hasText(rawKey) ? resilienceKeys.idempotency("wallet-" + action.toLowerCase(), rawKey.trim()) : null;
    }

    private void enforceAdminScope(UUID userId) {
        if (!currentUserProvider.isAuthenticated()) {
            return;
        }
        AppUser user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getBranch() == null) {
            branchScope.requireSuperAdmin();
            return;
        }
        branchScope.assertBranchAllowed(user.getBranch().getId());
    }

    private BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            throw new BusinessRuleException("Amount is required");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
