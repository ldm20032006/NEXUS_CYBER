package demo.server.repository.iot;

import demo.server.entity.iot.IotDevice;
import demo.server.common.enums.DeviceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IotDeviceRepository extends JpaRepository<IotDevice, UUID> {
    Optional<IotDevice> findBySerialNumber(String serialNumber);

    List<IotDevice> findByBranchId(UUID branchId);

    Page<IotDevice> findByBranchIdAndDeletedFalse(UUID branchId, Pageable pageable);

    Page<IotDevice> findByDeletedFalse(Pageable pageable);

    boolean existsBySerialNumber(String serialNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from IotDevice d where d.id = :id")
    Optional<IotDevice> findWithLockById(UUID id);

    @Query("select d.id from IotDevice d where d.deleted = false and d.status <> :offline and d.lastHeartbeatAt is not null and d.lastHeartbeatAt < :before order by d.lastHeartbeatAt asc")
    List<UUID> findHeartbeatTimedOutIds(DeviceStatus offline, Instant before, Pageable pageable);

    @Modifying
    @Query("update IotDevice d set d.status = :offline, d.missedHeartbeatCount = case when d.missedHeartbeatCount < 3 then 3 else d.missedHeartbeatCount end where d.id in :ids and d.status <> :offline")
    int markOfflineByIds(List<UUID> ids, DeviceStatus offline);
}
