package demo.server.dto.lobby;

import demo.server.common.enums.LobbyMemberRole;
import demo.server.common.enums.LobbyMemberStatus;

import java.time.Instant;
import java.util.UUID;

public record LobbyMemberResponse(
        UUID userId,
        LobbyMemberRole role,
        LobbyMemberStatus status,
        Instant joinedAt,
        Instant leftAt
) {
}
