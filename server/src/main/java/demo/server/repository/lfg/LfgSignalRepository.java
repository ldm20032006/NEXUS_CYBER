package demo.server.repository.lfg;

import demo.server.common.enums.LfgSignalStatus;
import demo.server.entity.lfg.LfgSignal;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LfgSignalRepository extends JpaRepository<LfgSignal, UUID> {
    List<LfgSignal> findByUserId(UUID userId);

    Optional<LfgSignal> findFirstByUser_IdAndStatusAndDeletedFalse(UUID userId, demo.server.common.enums.LfgSignalStatus status);

    List<LfgSignal> findByUser_IdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);

    @Query("""
            select s from LfgSignal s
            where s.deleted = false and s.status = demo.server.common.enums.LfgSignalStatus.ACTIVE
              and s.expiresAt > :now
              and s.branch.id = :branchId
              and (:gameId is null or s.game.id = :gameId)
              and (:rankId is null or s.rank.id = :rankId)
              and (:roleId is null or s.role.id = :roleId)
              and (:zoneId is null or s.zone.id = :zoneId)
            order by s.createdAt desc
            """)
    List<LfgSignal> searchActive(UUID branchId, UUID gameId, UUID rankId, UUID roleId, UUID zoneId, Instant now);

    List<LfgSignal> findByPlaySession_IdAndStatusAndDeletedFalse(UUID playSessionId, demo.server.common.enums.LfgSignalStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from LfgSignal s where s.id = :id")
    Optional<LfgSignal> findByIdForUpdate(UUID id);

    @Query("select s.id from LfgSignal s where s.status = :status and s.expiresAt <= :now order by s.expiresAt asc")
    List<UUID> findExpiredIds(LfgSignalStatus status, Instant now, Pageable pageable);

    @Modifying
    @Query("update LfgSignal s set s.status = :next where s.id in :ids and s.status = :expected")
    int updateStatusByIds(List<UUID> ids, LfgSignalStatus expected, LfgSignalStatus next);
}
