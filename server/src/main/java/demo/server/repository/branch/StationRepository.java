package demo.server.repository.branch;

import demo.server.entity.branch.Station;
import demo.server.common.enums.StationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StationRepository extends JpaRepository<Station, UUID>, JpaSpecificationExecutor<Station> {
    Optional<Station> findByCode(String code);

    List<Station> findByBranch_Id(UUID branchId);

    Optional<Station> findByBranch_IdAndCode(UUID branchId, String code);

    boolean existsByBranch_IdAndCode(UUID branchId, String code);

    boolean existsByBranch_IdAndStationNumber(UUID branchId, Integer stationNumber);

    @Query("select s.id from Station s where s.deleted = false and s.status <> :offline and s.lastSeenAt is not null and s.lastSeenAt < :before order by s.lastSeenAt asc")
    List<UUID> findHeartbeatTimedOutIds(StationStatus offline, Instant before, Pageable pageable);

    @Modifying
    @Query("update Station s set s.status = :offline where s.id in :ids and s.status <> :offline")
    int markOfflineByIds(List<UUID> ids, StationStatus offline);
}
