package demo.server.dto.game;

import demo.server.common.enums.GameStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GameRequest(
        @NotBlank @Size(max = 100) String slug,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String description,
        @NotNull @Min(1) @Max(20) Integer maxLobbySize,
        GameStatus status
) {
}
