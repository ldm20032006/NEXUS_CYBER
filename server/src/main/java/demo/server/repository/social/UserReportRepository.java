package demo.server.repository.social;

import demo.server.entity.social.UserReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserReportRepository extends JpaRepository<UserReport, UUID> {
    List<UserReport> findByReporterId(UUID reporterId);

    List<UserReport> findByReporter_IdAndDeletedFalseOrderByReportedAtDesc(UUID reporterId);

    List<UserReport> findByBranch_IdAndDeletedFalseOrderByReportedAtDesc(UUID branchId);

    List<UserReport> findByBranch_IdAndStatusAndDeletedFalseOrderByReportedAtDesc(UUID branchId, demo.server.common.enums.UserReportStatus status);

    Optional<UserReport> findByIdAndDeletedFalse(UUID id);
}
