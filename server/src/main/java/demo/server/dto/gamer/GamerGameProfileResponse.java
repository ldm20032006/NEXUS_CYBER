package demo.server.dto.gamer;

import java.util.UUID;

public record GamerGameProfileResponse(
        UUID id,
        UUID userId,
        UUID gameId,
        String gameName,
        String inGameName,
        UUID rankId,
        String rankName,
        UUID preferredRoleId,
        String preferredRoleName,
        UUID secondaryRoleId,
        String secondaryRoleName,
        String playStyle,
        String shortDescription,
        Boolean visibleOnRadar
) {
}
