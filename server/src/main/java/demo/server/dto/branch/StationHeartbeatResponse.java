package demo.server.dto.branch;

import demo.server.common.enums.StationStatus;

import java.time.Instant;
import java.util.UUID;

public record StationHeartbeatResponse(
        UUID stationId,
        StationStatus status,
        Instant lastSeenAt
) {
}
