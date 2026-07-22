package demo.server.repository.notification;

import demo.server.common.enums.NotificationDeliveryStatus;
import demo.server.entity.notification.NotificationDelivery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

    List<NotificationDelivery> findByNotificationIdOrderByCreatedAtAsc(UUID notificationId);

    List<NotificationDelivery> findByStatusAndAttemptCountLessThanAndNextAttemptAtBefore(NotificationDeliveryStatus status, int maxAttempts, Instant before);

    @Query("select d.id from NotificationDelivery d where d.status = :status and d.attemptCount < d.maxAttempts and d.nextAttemptAt <= :now order by d.nextAttemptAt asc")
    List<UUID> findRetryDueIds(NotificationDeliveryStatus status, Instant now, Pageable pageable);
}
