package demo.server.service.lfg;

import demo.server.dto.lfg.LfgSignalResponse;
import demo.server.dto.lfg.TeamInvitationResponse;
import demo.server.dto.lobby.LobbyMemberResponse;
import demo.server.dto.lobby.LobbyMessageResponse;
import demo.server.dto.lobby.LobbyResponse;
import demo.server.entity.lfg.LfgSignal;
import demo.server.entity.lfg.TeamInvitation;
import demo.server.entity.lobby.Lobby;
import demo.server.entity.lobby.LobbyMember;
import demo.server.entity.lobby.LobbyMessage;
import demo.server.repository.lobby.LobbyMemberRepository;
import org.springframework.stereotype.Component;

@Component
public class LfgMapper {

    private final LobbyMemberRepository lobbyMemberRepository;

    public LfgMapper(LobbyMemberRepository lobbyMemberRepository) {
        this.lobbyMemberRepository = lobbyMemberRepository;
    }

    public LfgSignalResponse toSignal(LfgSignal signal) {
        return new LfgSignalResponse(signal.getId(), signal.getUser().getId(), signal.getBranch().getId(),
                signal.getZone() == null ? null : signal.getZone().getId(), signal.getGame().getId(),
                signal.getRank() == null ? null : signal.getRank().getId(),
                signal.getRole() == null ? null : signal.getRole().getId(),
                signal.getTargetMembers(), signal.getMessage(), signal.getStatus(), signal.getExpiresAt());
    }

    public TeamInvitationResponse toInvitation(TeamInvitation invitation) {
        return new TeamInvitationResponse(invitation.getId(), invitation.getSender().getId(), invitation.getReceiver().getId(),
                invitation.getLobby() == null ? null : invitation.getLobby().getId(), invitation.getMessage(),
                invitation.getStatus(), invitation.getExpiresAt(), invitation.getRespondedAt());
    }

    public LobbyResponse toLobby(Lobby lobby) {
        return new LobbyResponse(lobby.getId(), lobby.getLeader().getId(), lobby.getBranch().getId(),
                lobby.getZone() == null ? null : lobby.getZone().getId(), lobby.getGame().getId(), lobby.getName(),
                lobby.getMaxMembers(), lobby.getStatus(), lobby.getVoiceStatus(), lobby.getVoiceProvider(), lobby.getVoiceChannelId(),
                lobbyMemberRepository.findByLobby_Id(lobby.getId()).stream().map(this::toMember).toList());
    }

    public LobbyMemberResponse toMember(LobbyMember member) {
        return new LobbyMemberResponse(member.getUser().getId(), member.getRole(), member.getStatus(),
                member.getJoinedAt(), member.getLeftAt());
    }

    public LobbyMessageResponse toMessage(LobbyMessage message) {
        return new LobbyMessageResponse(message.getId(), message.getLobby().getId(), message.getSender().getId(),
                message.getMessageType(), message.getContent(), message.getSentAt());
    }
}
