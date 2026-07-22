package demo.server.entity.iot;

import demo.server.common.entity.BaseEntity;
import demo.server.common.enums.DeviceCommandStatus;
import demo.server.common.enums.DeviceCommandType;
import demo.server.entity.auth.AppUser;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import demo.server.entity.session.PlaySession;
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
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "device_commands",
        uniqueConstraints = @UniqueConstraint(name = "uk_device_commands_correlation_id", columnNames = "correlation_id")
)
public class DeviceCommand extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private IotDevice device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "play_session_id")
    private PlaySession playSession;

    @Column(name = "correlation_id", nullable = false, unique = true)
    private UUID correlationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DeviceCommandType commandType;

    @Column(nullable = false, length = 200)
    private String commandValue;

    @Column(length = 30)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeviceCommandStatus status = DeviceCommandStatus.PENDING;

    @Column(nullable = false)
    private int attemptCount = 0;

    @Column(nullable = false)
    private int maxAttempts = 1;

    @Column(nullable = false)
    private boolean dangerous = false;

    @Column(nullable = false)
    private boolean emergency = false;

    @Column(nullable = false, length = 500)
    private String mqttTopic;

    private Instant sentAt;

    private Instant acknowledgedAt;

    private Instant timedOutAt;

    @Column(length = 1000)
    private String resultMessage;
}
