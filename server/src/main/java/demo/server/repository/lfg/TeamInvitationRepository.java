package demo.server.repository.lfg;

import demo.server.common.enums.InvitationStatus;
import demo.server.entity.lfg.TeamInvitation;
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

public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, UUID> {
    List<TeamInvitation> findBySenderIdOrReceiverId(UUID senderId, UUID receiverId);

    List<TeamInvitation> findByReceiver_IdAndDeletedFalseOrderByCreatedAtDesc(UUID receiverId);

    List<TeamInvitation> findBySender_IdAndDeletedFalseOrderByCreatedAtDesc(UUID senderId);

    boolean existsBySender_IdAndReceiver_IdAndStatusAndDeletedFalse(UUID senderId, UUID receiverId,
                                                                    demo.server.common.enums.InvitationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from TeamInvitation i where i.id = :id")
    Optional<TeamInvitation> findByIdForUpdate(UUID id);

    @Query("select i.id from TeamInvitation i where i.status = :status and i.expiresAt <= :now order by i.expiresAt asc")
    List<UUID> findExpiredIds(InvitationStatus status, Instant now, Pageable pageable);

    @Modifying
    @Query("update TeamInvitation i set i.status = :next, i.respondedAt = :now where i.id in :ids and i.status = :expected")
    int updateExpiredByIds(List<UUID> ids, InvitationStatus expected, InvitationStatus next, Instant now);
}
