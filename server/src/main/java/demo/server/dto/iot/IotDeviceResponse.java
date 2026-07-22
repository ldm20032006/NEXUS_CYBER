package demo.server.dto.iot;

import demo.server.common.enums.DeviceStatus;
import demo.server.common.enums.DeviceType;

import java.time.Instant;
import java.util.UUID;

public record IotDeviceResponse(
        UUID id,
        UUID branchId,
        UUID stationId,
        DeviceType deviceType,
        String serialNumber,
        String name,
        String firmwareVersion,
        String capabilities,
        DeviceStatus status,
        Instant lastHeartbeatAt,
        int missedHeartbeatCount,
        boolean mechanicalCommandLocked,
        String ipAddress,
        boolean deleted
) {
}
