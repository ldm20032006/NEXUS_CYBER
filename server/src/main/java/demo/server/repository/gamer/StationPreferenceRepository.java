package demo.server.repository.gamer;

import demo.server.entity.gamer.StationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StationPreferenceRepository extends JpaRepository<StationPreference, UUID> {
    Optional<StationPreference> findByUser_Id(UUID userId);
}
