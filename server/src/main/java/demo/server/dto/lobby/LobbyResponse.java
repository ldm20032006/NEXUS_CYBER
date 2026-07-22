package demo.server.dto.lobby;

import demo.server.common.enums.LobbyStatus;
import demo.server.common.enums.VoiceChannelStatus;

import java.util.List;
import java.util.UUID;

public record LobbyResponse(
        UUID id,
        UUID leaderId,
        UUID branchId,
        UUID zoneId,
        UUID gameId,
        String name,
        Integer maxMembers,
        LobbyStatus status,
        VoiceChannelStatus voiceStatus,
        String voiceProvider,
        String voiceChannelId,
        List<LobbyMemberResponse> members
) {
}
