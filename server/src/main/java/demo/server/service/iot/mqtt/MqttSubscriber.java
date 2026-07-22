package demo.server.service.iot.mqtt;

import demo.server.dto.iot.command.DeviceCommandAckRequest;

public interface MqttSubscriber {

    void handleAck(DeviceCommandAckRequest request);
}
