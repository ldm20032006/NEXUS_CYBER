package demo.server.entity.session;

import demo.server.common.entity.BaseEntity;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import demo.server.entity.branch.Zone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "session_billing_policies")
public class SessionBillingPolicy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal hourlyRate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal minimumCharge = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer billingIncrementMinutes = 1;

    @Column(nullable = false)
    private boolean active = true;
}
