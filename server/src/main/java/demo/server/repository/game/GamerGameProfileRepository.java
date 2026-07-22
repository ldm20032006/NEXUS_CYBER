package demo.server.repository.game;

import demo.server.entity.game.GamerGameProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GamerGameProfileRepository extends JpaRepository<GamerGameProfile, UUID> {
    List<GamerGameProfile> findByUser_Id(UUID userId);

    Optional<GamerGameProfile> findByUser_IdAndGame_Id(UUID userId, UUID gameId);

    boolean existsByUser_IdAndGame_Id(UUID userId, UUID gameId);
}
