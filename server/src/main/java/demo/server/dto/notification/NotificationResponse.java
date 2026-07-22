package demo.server.dto.notification;

import demo.server.common.enums.NotificationChannel;
import demo.server.common.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        NotificationChannel channel,
        String title,
        String content,
        Instant readAt,
        String entityType,
        String entityId,
        String branchId,
        Instant createdAt
) {
}
