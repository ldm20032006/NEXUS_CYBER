package demo.server.repository.branch;

import demo.server.entity.branch.StationCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StationCredentialRepository extends JpaRepository<StationCredential, UUID> {

    Optional<StationCredential> findFirstByStation_IdAndRevokedAtIsNullOrderByIssuedAtDesc(UUID stationId);

    List<StationCredential> findAllByStation_IdAndRevokedAtIsNull(UUID stationId);
}
