package demo.server.dto.lfg;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TeamInvitationRequest(
        @NotNull UUID receiverId,
        UUID lobbyId,
        @Size(max = 1000) String message
) {
}
