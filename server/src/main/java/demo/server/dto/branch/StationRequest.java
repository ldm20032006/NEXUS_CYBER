package demo.server.dto.branch;

import demo.server.common.enums.StationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StationRequest(
        @NotNull UUID branchId,
        UUID zoneId,
        @NotNull Integer stationNumber,
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String name,
        StationStatus status,
        @Size(max = 100) String ipAddress,
        @Size(max = 100) String macAddress
) {
}
