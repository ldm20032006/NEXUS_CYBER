package demo.server.entity.session;

import demo.server.common.entity.BaseEntity;
import demo.server.common.enums.SessionStatus;
import demo.server.entity.auth.AppUser;
import demo.server.entity.branch.Station;
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
@Table(name = "play_sessions")
public class PlaySession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qr_login_session_id")
    private QrLoginSession qrLoginSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SessionStatus status = SessionStatus.PENDING;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant endedAt;

    private Integer durationMinutes;

    @Column(precision = 19, scale = 2)
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal actualCost = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal startBalance = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal endBalance = BigDecimal.ZERO;

    @Column(length = 500)
    private String endedReason;

    @Column(length = 100)
    private String idempotencyKey;
}
