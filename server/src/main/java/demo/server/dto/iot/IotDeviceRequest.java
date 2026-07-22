package demo.server.dto.iot;

import demo.server.common.enums.DeviceStatus;
import demo.server.common.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record IotDeviceRequest(
        @NotNull UUID branchId,
        UUID stationId,
        @NotNull DeviceType deviceType,
        @NotBlank @Size(max = 100) String serialNumber,
        @Size(max = 150) String name,
        @Size(max = 100) String firmwareVersion,
        @Size(max = 2000) String capabilities,
        DeviceStatus status,
        @Size(max = 100) String ipAddress
) {
}
