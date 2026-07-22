package demo.server.repository.session;

import demo.server.entity.session.SessionBillingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionBillingPolicyRepository extends JpaRepository<SessionBillingPolicy, UUID> {

    List<SessionBillingPolicy> findByBranch_IdAndDeletedFalseOrderByCreatedAtDesc(UUID branchId);

    Optional<SessionBillingPolicy> findFirstByStation_IdAndActiveTrueAndDeletedFalseOrderByCreatedAtDesc(UUID stationId);

    Optional<SessionBillingPolicy> findFirstByZone_IdAndStationIsNullAndActiveTrueAndDeletedFalseOrderByCreatedAtDesc(UUID zoneId);

    Optional<SessionBillingPolicy> findFirstByBranch_IdAndZoneIsNullAndStationIsNullAndActiveTrueAndDeletedFalseOrderByCreatedAtDesc(UUID branchId);
}
