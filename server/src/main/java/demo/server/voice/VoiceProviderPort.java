package demo.server.voice;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public interface VoiceProviderPort {
    VoiceChannel createChannel(UUID lobbyId, String lobbyName);

    VoiceToken issueToken(String channelId, UUID lobbyId, UUID userId, Duration ttl);

    void closeChannel(String channelId);

    String providerName();

    String adapterMode();

    record VoiceToken(String token, Instant expiresAt) {
    }
}
