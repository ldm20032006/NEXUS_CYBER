package demo.server.repository.iot;

import demo.server.entity.iot.DeviceTelemetry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DeviceTelemetryRepository extends JpaRepository<DeviceTelemetry, UUID> {
    List<DeviceTelemetry> findByDeviceIdOrderByReceivedAtDesc(UUID deviceId);

    Page<DeviceTelemetry> findByDeviceIdAndReceivedAtBetweenOrderByReceivedAtDesc(UUID deviceId, Instant from, Instant to, Pageable pageable);
}
