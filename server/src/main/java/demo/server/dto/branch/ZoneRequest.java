package demo.server.dto.branch;

import demo.server.common.enums.ZoneStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ZoneRequest(
        @NotNull UUID branchId,
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 100) String zoneType,
        ZoneStatus status,
        Integer sortOrder
) {
}
