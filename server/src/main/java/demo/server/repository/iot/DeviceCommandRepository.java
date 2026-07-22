package demo.server.repository.iot;

import demo.server.common.enums.DeviceCommandStatus;
import demo.server.entity.iot.DeviceCommand;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, UUID> {

    Optional<DeviceCommand> findByCorrelationId(UUID correlationId);

    Page<DeviceCommand> findByStationIdOrderByCreatedAtDesc(UUID stationId, Pageable pageable);

    List<DeviceCommand> findByStatusAndSentAtBefore(DeviceCommandStatus status, Instant before);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from DeviceCommand c where c.correlationId = :correlationId")
    Optional<DeviceCommand> findWithLockByCorrelationId(UUID correlationId);
}
