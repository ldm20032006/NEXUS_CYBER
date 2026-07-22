package demo.server.service.audit;

import demo.server.common.enums.AuditAction;
import demo.server.common.response.PageResponse;
import demo.server.dto.audit.AuditLogResponse;
import demo.server.entity.audit.AuditLog;
import demo.server.repository.audit.AuditLogRepository;
import demo.server.service.branch.BranchScope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final BranchScope branchScope;

    public AuditLogService(AuditLogRepository auditLogRepository, BranchScope branchScope) {
        this.auditLogRepository = auditLogRepository;
        this.branchScope = branchScope;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(UUID branchId, AuditAction action, String resourceType, int page, int size) {
        UUID scopedBranchId = branchScope.requireScopedBranch(branchId);
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "timestamp"));
        Specification<AuditLog> spec = Specification.allOf(branchFilter(scopedBranchId), actionFilter(action), resourceFilter(resourceType));
        return PageResponse.from(auditLogRepository.findAll(spec, pageable).map(this::toResponse));
    }

    private Specification<AuditLog> branchFilter(UUID branchId) {
        return (root, query, criteriaBuilder) -> branchId == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("branch").get("id"), branchId);
    }

    private Specification<AuditLog> actionFilter(AuditAction action) {
        return (root, query, criteriaBuilder) -> action == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("action"), action);
    }

    private Specification<AuditLog> resourceFilter(String resourceType) {
        return (root, query, criteriaBuilder) -> resourceType == null || resourceType.isBlank()
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(criteriaBuilder.lower(root.get("resourceType")), resourceType.trim().toLowerCase());
    }

    private AuditLogResponse toResponse(AuditLog log) {
        UUID actorId = log.getActor() == null ? null : log.getActor().getId();
        UUID branchId = log.getBranch() == null ? null : log.getBranch().getId();
        return new AuditLogResponse(
                log.getId(),
                actorId,
                log.getActorRole(),
                branchId,
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getBeforeData(),
                log.getAfterData(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getCorrelationId(),
                log.getTimestamp()
        );
    }
}
