package demo.server.dto.lobby;

import demo.server.common.enums.MessageType;

import java.time.Instant;
import java.util.UUID;

public record LobbyMessageResponse(
        UUID id,
        UUID lobbyId,
        UUID senderId,
        MessageType messageType,
        String content,
        Instant sentAt
) {
}
