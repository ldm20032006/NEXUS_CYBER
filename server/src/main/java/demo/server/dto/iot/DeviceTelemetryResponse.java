package demo.server.dto.iot;

import java.time.Instant;
import java.util.UUID;

public record DeviceTelemetryResponse(
        UUID id,
        UUID deviceId,
        UUID branchId,
        UUID stationId,
        Instant receivedAt,
        Boolean online,
        Integer batteryLevel,
        Integer signalStrength,
        String errorCode,
        String firmwareVersion,
        String metricKey,
        String metricValue,
        String payloadJson
) {
}
