package demo.server.dto.iot.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DeviceCommandAckRequest(
        @NotNull UUID branchId,
        @NotNull UUID stationId,
        @NotNull UUID deviceId,
        @NotNull UUID correlationId,
        boolean success,
        @Size(max = 1000) String message
) {
}
