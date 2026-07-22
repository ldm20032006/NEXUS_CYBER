package demo.server.voice;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "nexus.voice")
public record VoiceProperties(
        String provider,
        String providerKey,
        String webhookSecret,
        Duration tokenTtl,
        Duration webhookReplayWindow,
        boolean mockFailure
) {
    public VoiceProperties {
        if (provider == null || provider.isBlank()) {
            provider = "mock";
        }
        if (tokenTtl == null) {
            tokenTtl = Duration.ofMinutes(5);
        }
        if (webhookReplayWindow == null) {
            webhookReplayWindow = Duration.ofMinutes(5);
        }
    }
}
