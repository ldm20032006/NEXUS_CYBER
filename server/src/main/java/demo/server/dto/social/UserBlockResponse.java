package demo.server.dto.social;

import java.time.Instant;
import java.util.UUID;

public record UserBlockResponse(
        UUID id,
        UUID blockedUserId,
        String reason,
        Instant blockedAt
) {
}
