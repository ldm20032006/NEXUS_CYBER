package demo.server.dto.gamer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record GamerGameProfileRequest(
        @NotNull UUID gameId,
        @Size(max = 120) String inGameName,
        UUID rankId,
        UUID preferredRoleId,
        UUID secondaryRoleId,
        @Size(max = 1000) String playStyle,
        @Size(max = 1000) String shortDescription,
        Boolean visibleOnRadar
) {
}
