package demo.server.service.iot.mqtt;

import demo.server.common.enums.DeviceCommandType;
import demo.server.dto.iot.command.DeviceCommandPayload;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "nexus.mqtt.provider", havingValue = "mock", matchIfMissing = true)
public class MockMqttPublisher implements MqttPublisher {

    @Override
    public MqttPublishResult publish(String topic, DeviceCommandPayload payload) {
        if (payload.type() == DeviceCommandType.EMERGENCY_STOP) {
            return MqttPublishResult.acknowledgedSuccess();
        }
        if ("TIMEOUT".equalsIgnoreCase(payload.value())) {
            return MqttPublishResult.acceptedOnly();
        }
        if ("FAIL".equalsIgnoreCase(payload.value())) {
            return MqttPublishResult.failed("Mock MQTT publish failed");
        }
        return MqttPublishResult.acknowledgedSuccess();
    }
}
