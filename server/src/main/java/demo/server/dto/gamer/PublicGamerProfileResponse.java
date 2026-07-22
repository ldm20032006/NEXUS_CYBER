package demo.server.dto.gamer;

import java.util.UUID;

public record PublicGamerProfileResponse(
        UUID userId,
        String nickname,
        String avatarUrl,
        String bio
) {
}
