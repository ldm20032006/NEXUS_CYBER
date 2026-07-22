package demo.server.dto.game;

import demo.server.common.enums.GameStatus;

import java.util.List;
import java.util.UUID;

public record GameResponse(
        UUID id,
        String slug,
        String name,
        String description,
        Integer maxLobbySize,
        GameStatus status,
        List<GameRankResponse> ranks,
        List<GameRoleResponse> roles
) {
}
