package demo.server.repository.wallet;

import demo.server.entity.wallet.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId);

    Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<WalletTransaction> findFirstByReferenceTypeAndReferenceIdAndType(String referenceType, String referenceId,
                                                                               demo.server.common.enums.TransactionType type);

    boolean existsByOriginalTransaction_Id(UUID originalTransactionId);

    @Query("select coalesce(sum(t.amount), 0) from WalletTransaction t where t.originalTransaction.id = :originalTransactionId")
    java.math.BigDecimal sumRefundedAmount(UUID originalTransactionId);
}
