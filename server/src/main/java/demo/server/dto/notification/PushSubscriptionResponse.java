package demo.server.dto.notification;

import java.time.Instant;
import java.util.UUID;

public record PushSubscriptionResponse(
        UUID id,
        UUID userId,
        String endpoint,
        String userAgent,
        Instant lastUsedAt,
        boolean deleted
) {
}
