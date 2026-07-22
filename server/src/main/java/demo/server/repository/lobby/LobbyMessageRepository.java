package demo.server.repository.lobby;

import demo.server.entity.lobby.LobbyMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LobbyMessageRepository extends JpaRepository<LobbyMessage, UUID> {
    List<LobbyMessage> findByLobbyIdOrderBySentAtAsc(UUID lobbyId);

    Page<LobbyMessage> findByLobby_IdOrderBySentAtDesc(UUID lobbyId, Pageable pageable);

    @Modifying
    @Query("update LobbyMessage m set m.deleted = true, m.deletedAt = :now where m.sentAt < :before and m.deleted = false")
    int softDeleteOlderThan(Instant before, Instant now);
}
