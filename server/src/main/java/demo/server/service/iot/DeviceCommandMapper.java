package demo.server.service.iot;

import demo.server.dto.iot.command.CommandHistoryResponse;
import demo.server.dto.iot.command.DeviceCommandResponse;
import demo.server.entity.iot.CommandHistory;
import demo.server.entity.iot.DeviceCommand;
import org.springframework.stereotype.Component;

@Component
public class DeviceCommandMapper {

    public DeviceCommandResponse toCommand(DeviceCommand command) {
        return new DeviceCommandResponse(
                command.getId(),
                command.getBranch().getId(),
                command.getStation().getId(),
                command.getDevice().getId(),
                command.getCorrelationId(),
                command.getCommandType(),
                command.getCommandValue(),
                command.getUnit(),
                command.getStatus(),
                command.getAttemptCount(),
                command.getMaxAttempts(),
                command.isDangerous(),
                command.isEmergency(),
                command.getMqttTopic(),
                command.getSentAt(),
                command.getAcknowledgedAt(),
                command.getResultMessage());
    }

    public CommandHistoryResponse toHistory(CommandHistory history) {
        return new CommandHistoryResponse(
                history.getId(),
                history.getCommand().getId(),
                history.getActor() == null ? null : history.getActor().getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getAction(),
                history.getNote(),
                history.getCreatedAt());
    }
}
