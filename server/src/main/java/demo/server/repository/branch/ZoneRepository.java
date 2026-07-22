package demo.server.repository.branch;

import demo.server.entity.branch.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ZoneRepository extends JpaRepository<Zone, UUID>, JpaSpecificationExecutor<Zone> {
    List<Zone> findByBranch_Id(UUID branchId);

    Optional<Zone> findByBranch_IdAndCode(UUID branchId, String code);

    boolean existsByBranch_IdAndCode(UUID branchId, String code);
}
