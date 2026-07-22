package demo.server.dto.lfg;

import demo.server.common.enums.InvitationStatus;

import java.time.Instant;
import java.util.UUID;

public record TeamInvitationResponse(
        UUID id,
        UUID senderId,
        UUID receiverId,
        UUID lobbyId,
        String message,
        InvitationStatus status,
        Instant expiresAt,
        Instant respondedAt
) {
}
