package demo.server.common.resilience;

import java.time.Instant;

public record IdempotencyRecord(
        String key,
        String fingerprint,
        IdempotencyStatus status,
        Integer statusCode,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt
) {
}
