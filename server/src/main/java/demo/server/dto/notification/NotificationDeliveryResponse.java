package demo.server.dto.notification;

import demo.server.common.enums.NotificationChannel;
import demo.server.common.enums.NotificationDeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationDeliveryResponse(
        UUID id,
        UUID notificationId,
        UUID pushSubscriptionId,
        NotificationChannel channel,
        NotificationDeliveryStatus status,
        int attemptCount,
        int maxAttempts,
        Instant lastAttemptAt,
        Instant nextAttemptAt,
        String failureReason
) {
}
