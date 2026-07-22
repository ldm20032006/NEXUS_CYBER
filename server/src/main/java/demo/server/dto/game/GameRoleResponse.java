package demo.server.dto.game;

import java.util.UUID;

public record GameRoleResponse(
        UUID id,
        UUID gameId,
        String code,
        String name,
        Integer sortOrder
) {
}
