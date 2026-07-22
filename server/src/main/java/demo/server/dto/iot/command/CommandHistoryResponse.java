package demo.server.dto.iot.command;

import demo.server.common.enums.DeviceCommandStatus;

import java.time.Instant;
import java.util.UUID;

public record CommandHistoryResponse(
        UUID id,
        UUID commandId,
        UUID actorId,
        DeviceCommandStatus fromStatus,
        DeviceCommandStatus toStatus,
        String action,
        String note,
        Instant createdAt
) {
}
