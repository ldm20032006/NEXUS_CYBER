package demo.server.dto.branch;

import demo.server.common.enums.ZoneStatus;

import java.util.UUID;

public record ZoneResponse(
        UUID id,
        UUID branchId,
        String code,
        String name,
        String zoneType,
        ZoneStatus status,
        Integer sortOrder
) {
}
