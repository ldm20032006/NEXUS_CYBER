package demo.server.repository.branch;

import demo.server.entity.branch.Branch;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID>, JpaSpecificationExecutor<Branch> {
    Optional<Branch> findByCode(String code);

    boolean existsByCode(String code);
}
