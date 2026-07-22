package demo.server.entity.ordering;

import demo.server.common.entity.BaseEntity;
import demo.server.common.enums.OrderStatus;
import demo.server.common.enums.PaymentMethod;
import demo.server.entity.auth.AppUser;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import demo.server.entity.session.PlaySession;
import demo.server.entity.wallet.WalletTransaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "food_orders")
public class FoodOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "play_session_id", nullable = false)
    private PlaySession playSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod paymentMethod = PaymentMethod.WALLET;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 1000)
    private String note;

    @Column(length = 500)
    private String cancelReason;

    @Column(length = 100)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_wallet_transaction_id")
    private WalletTransaction paymentWalletTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_wallet_transaction_id")
    private WalletTransaction refundWalletTransaction;

    private Instant acceptedAt;

    private Instant preparingAt;

    private Instant readyAt;

    private Instant deliveredAt;

    private Instant cancelledAt;
}
