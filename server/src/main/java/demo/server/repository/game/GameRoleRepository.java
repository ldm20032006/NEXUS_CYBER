package demo.server.repository.game;

import demo.server.entity.game.GameRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRoleRepository extends JpaRepository<GameRole, UUID> {
    List<GameRole> findByGame_Id(UUID gameId);

    boolean existsByGame_IdAndCode(UUID gameId, String code);

    Optional<GameRole> findByGame_IdAndCode(UUID gameId, String code);
}
