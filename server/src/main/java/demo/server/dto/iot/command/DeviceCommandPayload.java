package demo.server.dto.iot.command;

import demo.server.common.enums.DeviceCommandType;

import java.time.Instant;
import java.util.UUID;

public record DeviceCommandPayload(
        UUID commandId,
        UUID correlationId,
        DeviceCommandType type,
        String value,
        String unit,
        Instant timestamp
) {
}
