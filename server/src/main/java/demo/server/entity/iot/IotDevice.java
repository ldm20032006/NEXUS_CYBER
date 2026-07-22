package demo.server.entity.iot;

import demo.server.common.entity.BaseEntity;
import demo.server.common.enums.DeviceStatus;
import demo.server.common.enums.DeviceType;
import demo.server.entity.branch.Branch;
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
        name = "iot_devices",
        uniqueConstraints = @UniqueConstraint(name = "uk_iot_device_serial", columnNames = "serial_number")
)
public class IotDevice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DeviceType deviceType;

    @Column(name = "serial_number", nullable = false, unique = true, length = 100)
    private String serialNumber;

    @Column(length = 100)
    private String firmwareVersion;

    @Column(length = 150)
    private String name;

    @Column(length = 2000)
    private String capabilities;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeviceStatus status = DeviceStatus.ACTIVE;

    private Instant lastHeartbeatAt;

    @Column(nullable = false)
    private int missedHeartbeatCount = 0;

    @Column(nullable = false)
    private boolean mechanicalCommandLocked = false;

    @Column(length = 100)
    private String ipAddress;
}
