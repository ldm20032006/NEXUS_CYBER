package demo.server.dto.lfg;

import demo.server.common.enums.LfgSignalStatus;

import java.time.Instant;
import java.util.UUID;

public record LfgSignalResponse(
        UUID id,
        UUID userId,
        UUID branchId,
        UUID zoneId,
        UUID gameId,
        UUID rankId,
        UUID roleId,
        Integer targetMembers,
        String message,
        LfgSignalStatus status,
        Instant expiresAt
) {
}
