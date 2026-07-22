package demo.server.dto.lfg;

import java.util.UUID;

public record LfgSearchRequest(
        UUID branchId,
        UUID gameId,
        UUID rankId,
        UUID roleId,
        UUID zoneId
) {
}
