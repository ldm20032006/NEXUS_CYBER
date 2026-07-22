package demo.server.repository.iot;

import demo.server.common.enums.AlertStatus;
import demo.server.entity.iot.DeviceAlert;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface DeviceAlertRepository extends JpaRepository<DeviceAlert, UUID> {
    List<DeviceAlert> findByBranchIdAndStatusOrderByCreatedAtDesc(UUID branchId, AlertStatus status);

    Page<DeviceAlert> findByBranchIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(UUID branchId, AlertStatus status, Pageable pageable);

    Page<DeviceAlert> findByBranchIdAndDeletedFalseOrderByCreatedAtDesc(UUID branchId, Pageable pageable);

    Page<DeviceAlert> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByDeviceIdAndAlertCodeAndStatusInAndDeletedFalse(UUID deviceId, String alertCode, List<AlertStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from DeviceAlert a where a.id = :id")
    Optional<DeviceAlert> findWithLockById(UUID id);

    @Query("select a from DeviceAlert a where a.device.id = :deviceId and a.alertCode = :alertCode and a.status in :statuses and a.deleted = false")
    List<DeviceAlert> findOpenDuplicates(UUID deviceId, String alertCode, List<AlertStatus> statuses);
}
