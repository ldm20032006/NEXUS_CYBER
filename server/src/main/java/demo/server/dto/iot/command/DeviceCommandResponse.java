package demo.server.dto.iot.command;

import demo.server.common.enums.DeviceCommandStatus;
import demo.server.common.enums.DeviceCommandType;

import java.time.Instant;
import java.util.UUID;

public record DeviceCommandResponse(
        UUID id,
        UUID branchId,
        UUID stationId,
        UUID deviceId,
        UUID correlationId,
        DeviceCommandType commandType,
        String value,
        String unit,
        DeviceCommandStatus status,
        int attemptCount,
        int maxAttempts,
        boolean dangerous,
        boolean emergency,
        String mqttTopic,
        Instant sentAt,
        Instant acknowledgedAt,
        String resultMessage
) {
}
