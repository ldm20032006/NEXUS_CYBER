package demo.server.repository.payment;

import demo.server.entity.payment.PaymentTransaction;
import demo.server.common.enums.PaymentTransactionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByProviderAndProviderTransactionId(String provider, String providerTransactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentTransaction p where p.provider = :provider and p.providerTransactionId = :providerTransactionId")
    Optional<PaymentTransaction> findByProviderTransactionForUpdate(String provider, String providerTransactionId);

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    @Query("select p.id from PaymentTransaction p where p.status in :statuses and p.requestedAt < :before order by p.requestedAt asc")
    List<UUID> findReconciliationCandidateIds(List<PaymentTransactionStatus> statuses, Instant before, Pageable pageable);
}
