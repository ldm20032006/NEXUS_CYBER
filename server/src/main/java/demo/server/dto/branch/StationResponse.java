package demo.server.dto.branch;

import demo.server.common.enums.StationStatus;

import java.time.Instant;
import java.util.UUID;

public record StationResponse(
        UUID id,
        UUID branchId,
        UUID zoneId,
        Integer stationNumber,
        String code,
        String name,
        StationStatus status,
        String ipAddress,
        String macAddress,
        Instant lastSeenAt
) {
}
