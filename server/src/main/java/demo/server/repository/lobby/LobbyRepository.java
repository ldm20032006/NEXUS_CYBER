package demo.server.repository.lobby;

import demo.server.entity.lobby.Lobby;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LobbyRepository extends JpaRepository<Lobby, UUID> {
    List<Lobby> findByLeaderId(UUID leaderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lobby l where l.id = :id")
    Optional<Lobby> findByIdForUpdate(UUID id);
}
