package demo.server.dto.iot.command;

import demo.server.common.enums.CommandBatchStatus;

import java.util.List;
import java.util.UUID;

public record ApplyStationPreferenceResponse(
        UUID stationId,
        UUID playSessionId,
        CommandBatchStatus status,
        int total,
        int success,
        int failed,
        int skipped,
        List<DeviceCommandResponse> commands
) {
}
