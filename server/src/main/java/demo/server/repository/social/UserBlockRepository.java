package demo.server.repository.social;

import demo.server.entity.social.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {
    List<UserBlock> findByBlockerId(UUID blockerId);

    List<UserBlock> findByBlocker_IdAndDeletedFalse(UUID blockerId);

    Optional<UserBlock> findByBlocker_IdAndBlockedUser_IdAndDeletedFalse(UUID blockerId, UUID blockedUserId);

    Optional<UserBlock> findByBlocker_IdAndBlockedUser_Id(UUID blockerId, UUID blockedUserId);

    boolean existsByBlocker_IdAndBlockedUser_IdAndDeletedFalse(UUID blockerId, UUID blockedUserId);
}
