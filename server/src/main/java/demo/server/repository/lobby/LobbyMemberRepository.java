package demo.server.repository.lobby;

import demo.server.entity.lobby.LobbyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LobbyMemberRepository extends JpaRepository<LobbyMember, UUID> {
    List<LobbyMember> findByLobby_Id(UUID lobbyId);

    List<LobbyMember> findByLobby_IdAndStatus(UUID lobbyId, demo.server.common.enums.LobbyMemberStatus status);

    Optional<LobbyMember> findByLobby_IdAndUser_Id(UUID lobbyId, UUID userId);

    Optional<LobbyMember> findByLobby_IdAndUser_IdAndStatus(UUID lobbyId, UUID userId,
                                                            demo.server.common.enums.LobbyMemberStatus status);

    long countByLobby_IdAndStatus(UUID lobbyId, demo.server.common.enums.LobbyMemberStatus status);
}
