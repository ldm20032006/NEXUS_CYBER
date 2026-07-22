package demo.server.service.iot.mqtt;

import demo.server.dto.iot.command.DeviceCommandPayload;

public interface MqttPublisher {

    MqttPublishResult publish(String topic, DeviceCommandPayload payload);
}
