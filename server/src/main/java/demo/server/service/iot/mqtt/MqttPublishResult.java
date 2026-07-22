package demo.server.service.iot.mqtt;

public record MqttPublishResult(
        boolean accepted,
        boolean acknowledged,
        boolean success,
        String message
) {
    public static MqttPublishResult acknowledgedSuccess() {
        return new MqttPublishResult(true, true, true, "Mock MQTT ACK success");
    }

    public static MqttPublishResult acceptedOnly() {
        return new MqttPublishResult(true, false, false, "Command published; ACK pending");
    }

    public static MqttPublishResult failed(String message) {
        return new MqttPublishResult(false, false, false, message);
    }
}
