package demo.server.repository.game;

import demo.server.entity.game.GameRank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRankRepository extends JpaRepository<GameRank, UUID> {
    List<GameRank> findByGame_Id(UUID gameId);

    boolean existsByGame_IdAndCode(UUID gameId, String code);

    Optional<GameRank> findByGame_IdAndCode(UUID gameId, String code);
}
