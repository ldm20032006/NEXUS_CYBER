package demo.server.entity.branch;

import demo.server.common.entity.BaseEntity;
import demo.server.common.enums.StationStatus;
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
        name = "stations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_station_branch_code", columnNames = {"branch_id", "code"}),
                @UniqueConstraint(name = "uk_station_branch_number", columnNames = {"branch_id", "station_number"})
        }
)
public class Station extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @Column(name = "station_number", nullable = false)
    private Integer stationNumber;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StationStatus status = StationStatus.AVAILABLE;

    @Column(length = 100)
    private String ipAddress;

    @Column(length = 100)
    private String macAddress;

    private Instant lastSeenAt;
}
