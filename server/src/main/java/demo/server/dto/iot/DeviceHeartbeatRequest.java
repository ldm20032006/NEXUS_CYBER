package demo.server.dto.iot;

import jakarta.validation.constraints.Size;

public record DeviceHeartbeatRequest(
        @Size(max = 100) String firmwareVersion,
        @Size(max = 100) String ipAddress
) {
}
