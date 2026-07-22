package demo.server.entity.session;

import demo.server.common.entity.BaseEntity;
import demo.server.common.enums.QrLoginSessionStatus;
import demo.server.entity.branch.Station;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "qr_login_sessions",
        uniqueConstraints = @UniqueConstraint(name = "uk_qr_nonce", columnNames = "nonce")
)
public class QrLoginSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(nullable = false, length = 100, unique = true)
    private String nonce;

    @Column(nullable = false, length = 500)
    private String qrPayload;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant consumedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QrLoginSessionStatus status = QrLoginSessionStatus.PENDING;

    @Column(length = 100)
    private String idempotencyKey;
}
