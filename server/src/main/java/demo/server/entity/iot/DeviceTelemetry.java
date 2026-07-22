package demo.server.entity.iot;

import demo.server.common.entity.BaseEntity;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "device_telemetries")
public class DeviceTelemetry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private IotDevice device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;

    @Column(nullable = false)
    private Instant receivedAt;

    private Boolean online;

    private Integer batteryLevel;

    private Integer signalStrength;

    @Column(length = 100)
    private String errorCode;

    @Column(length = 100)
    private String firmwareVersion;

    @Column(length = 100)
    private String metricKey;

    @Column(length = 200)
    private String metricValue;

    @Lob
    @Column(columnDefinition = "text")
    private String payloadJson;
}
