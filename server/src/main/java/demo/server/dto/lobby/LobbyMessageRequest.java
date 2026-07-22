package demo.server.dto.lobby;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LobbyMessageRequest(
        @NotBlank @Size(max = 1000) String content
) {
}
