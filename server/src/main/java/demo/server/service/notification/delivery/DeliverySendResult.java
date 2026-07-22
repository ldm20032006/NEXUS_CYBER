package demo.server.service.notification.delivery;

public record DeliverySendResult(
        boolean sent,
        String message
) {
    public static DeliverySendResult sent(String message) {
        return new DeliverySendResult(true, message);
    }

    public static DeliverySendResult failed(String message) {
        return new DeliverySendResult(false, message);
    }
}
