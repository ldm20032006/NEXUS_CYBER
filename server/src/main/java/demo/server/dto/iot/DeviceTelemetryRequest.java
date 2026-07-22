package demo.server.dto.iot;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record DeviceTelemetryRequest(
        Boolean online,
        @Min(0) @Max(100) Integer batteryLevel,
        @Min(0) @Max(100) Integer signalStrength,
        @Size(max = 100) String errorCode,
        @Size(max = 100) String firmwareVersion,
        @Size(max = 100) String metricKey,
        @Size(max = 200) String metricValue,
        @Size(max = 10000) String payloadJson
) {
}
