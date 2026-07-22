package demo.server.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "nexus.payment")
public record PaymentProperties(
        String provider,
        String webhookSecret,
        Duration webhookReplayWindow
) {
    public PaymentProperties {
        if (provider == null || provider.isBlank()) {
            provider = "mock";
        }
        if (webhookReplayWindow == null) {
            webhookReplayWindow = Duration.ofMinutes(5);
        }
    }
}
