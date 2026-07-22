package demo.server.entity.payment;

import demo.server.common.entity.BaseEntity;
import demo.server.common.enums.PaymentTransactionStatus;
import demo.server.entity.auth.AppUser;
import demo.server.entity.wallet.WalletTransaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "payment_transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_transactions_provider_tx", columnNames = {"provider", "provider_transaction_id"}),
                @UniqueConstraint(name = "uk_payment_transactions_idempotency_key", columnNames = {"idempotency_key"})
        }
)
public class PaymentTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_transaction_id", nullable = false, length = 120)
    private String providerTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentTransactionStatus status = PaymentTransactionStatus.PENDING;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency = "VND";

    @Column(length = 120)
    private String idempotencyKey;

    @Column(length = 500)
    private String checkoutUrl;

    @Column(nullable = false)
    private Instant requestedAt;

    private Instant processedAt;

    @Column(length = 500)
    private String failureReason;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_transaction_id")
    private WalletTransaction walletTransaction;
}
