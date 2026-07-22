package demo.server.repository.notification;

import demo.server.entity.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<Notification> findByUserIdAndHiddenAtIsNullAndDeletedFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndReadAtIsNullAndHiddenAtIsNullAndDeletedFalse(UUID userId);

    boolean existsByUserIdAndTypeAndEntityTypeAndEntityIdAndDeletedFalse(UUID userId,
                                                                         demo.server.common.enums.NotificationType type,
                                                                         String entityType,
                                                                         String entityId);

    @Modifying
    @Query("update Notification n set n.readAt = :readAt where n.user.id = :userId and n.readAt is null and n.hiddenAt is null and n.deleted = false")
    int markAllRead(UUID userId, Instant readAt);

    @Modifying
    @Query("update Notification n set n.deleted = true, n.deletedAt = :now, n.hiddenAt = coalesce(n.hiddenAt, :now) where n.createdAt < :before and n.deleted = false")
    int softDeleteOlderThan(Instant before, Instant now);
}
