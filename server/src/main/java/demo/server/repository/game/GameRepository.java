package demo.server.repository.game;

import demo.server.entity.game.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID>, JpaSpecificationExecutor<Game> {
    Optional<Game> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
