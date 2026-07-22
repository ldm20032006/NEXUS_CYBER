package demo.server.common.resilience;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ResilienceKeys {

    private final ResilienceProperties properties;

    public ResilienceKeys(ResilienceProperties properties) {
        this.properties = properties;
    }

    public String rateLimit(String action, String subject) {
        return prefixed("rl", action, subject);
    }

    public String idempotency(String action, String idempotencyKey) {
        return prefixed("idem", action, idempotencyKey);
    }

    public String lockQr(UUID qrSessionId) {
        return prefixed("lock", "qr", qrSessionId.toString());
    }

    public String lockSession(UUID sessionId) {
        return prefixed("lock", "session", sessionId.toString());
    }

    public String lockWallet(UUID walletId) {
        return prefixed("lock", "wallet", walletId.toString());
    }

    public String lockPaymentCallback(String provider, String externalReference) {
        return prefixed("lock", "payment-callback", provider, externalReference);
    }

    public String lockStock(UUID menuItemId) {
        return prefixed("lock", "stock", menuItemId.toString());
    }

    public String lockDevice(UUID deviceId) {
        return prefixed("lock", "device", deviceId.toString());
    }

    public String lockDeviceAlert(UUID alertId) {
        return prefixed("lock", "device-alert", alertId.toString());
    }

    public String lockDeviceCommand(UUID commandId) {
        return prefixed("lock", "device-command", commandId.toString());
    }

    public String lockDeviceCommandCorrelation(UUID correlationId) {
        return prefixed("lock", "device-command-correlation", correlationId.toString());
    }

    public String lockJob(String jobName) {
        return prefixed("lock", "job", jobName);
    }

    public String lockInvitationAccept(UUID invitationId) {
        return prefixed("lock", "invitation-accept", invitationId.toString());
    }

    public String onlineUser(UUID userId) {
        return prefixed("online", "user", userId.toString());
    }

    public String onlineStation(UUID stationId) {
        return prefixed("online", "station", stationId.toString());
    }

    private String prefixed(String... parts) {
        return properties.keyPrefix() + ":" + String.join(":", parts);
    }
}
