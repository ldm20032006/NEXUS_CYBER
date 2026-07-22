package demo.server.repository.gamer;

import demo.server.entity.gamer.GamerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GamerProfileRepository extends JpaRepository<GamerProfile, UUID> {
    Optional<GamerProfile> findByUser_Id(UUID userId);
}
