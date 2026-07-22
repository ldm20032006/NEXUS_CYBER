package demo.server.dto.iot;

import demo.server.common.enums.AlertStatus;

import java.time.Instant;
import java.util.UUID;

public record AlertHistoryResponse(
        UUID id,
        UUID alertId,
        UUID actorId,
        AlertStatus fromStatus,
        AlertStatus toStatus,
        String action,
        String note,
        Instant createdAt
) {
}
