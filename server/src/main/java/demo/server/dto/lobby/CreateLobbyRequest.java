package demo.server.dto.lobby;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateLobbyRequest(
        @NotNull UUID gameId,
        @Size(max = 150) String name
) {
}
