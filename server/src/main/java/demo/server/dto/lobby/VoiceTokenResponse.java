package demo.server.dto.lobby;

import demo.server.common.enums.VoiceChannelStatus;

import java.time.Instant;
import java.util.UUID;

public record VoiceTokenResponse(
        UUID lobbyId,
        UUID userId,
        String provider,
        String channelId,
        String token,
        Instant expiresAt,
        VoiceChannelStatus status
) {
}
