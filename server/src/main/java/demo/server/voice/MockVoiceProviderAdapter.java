package demo.server.voice;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Component
@EnableConfigurationProperties(VoiceProperties.class)
public class MockVoiceProviderAdapter implements VoiceProviderPort {

    private final VoiceProperties properties;

    public MockVoiceProviderAdapter(VoiceProperties properties) {
        this.properties = properties;
    }

    @Override
    public VoiceChannel createChannel(UUID lobbyId, String lobbyName) {
        if (properties.mockFailure()) {
            throw new VoiceProviderException("Mock voice provider is unavailable");
        }
        return new VoiceChannel(providerName(), "mock-voice-" + lobbyId);
    }

    @Override
    public VoiceToken issueToken(String channelId, UUID lobbyId, UUID userId, Duration ttl) {
        if (properties.mockFailure()) {
            throw new VoiceProviderException("Mock voice provider is unavailable");
        }
        Instant expiresAt = Instant.now().plus(ttl);
        String tokenPayload = "mock:" + channelId + ":" + lobbyId + ":" + userId + ":" + expiresAt;
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(tokenPayload.getBytes(StandardCharsets.UTF_8));
        return new VoiceToken(token, expiresAt);
    }

    @Override
    public void closeChannel(String channelId) {
        if (properties.mockFailure()) {
            throw new VoiceProviderException("Mock voice provider is unavailable");
        }
    }

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public String adapterMode() {
        return "development-mock";
    }
}
