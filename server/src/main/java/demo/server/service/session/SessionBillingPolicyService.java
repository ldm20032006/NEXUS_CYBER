package demo.server.service.session;

import demo.server.common.enums.AuditAction;
import demo.server.common.audit.AuditRecorder;
import demo.server.dto.session.SessionBillingPolicyRequest;
import demo.server.dto.session.SessionBillingPolicyResponse;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import demo.server.entity.branch.Zone;
import demo.server.entity.session.PlaySession;
import demo.server.entity.session.SessionBillingPolicy;
import demo.server.exception.BusinessRuleException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.repository.branch.BranchRepository;
import demo.server.repository.branch.StationRepository;
import demo.server.repository.branch.ZoneRepository;
import demo.server.repository.session.SessionBillingPolicyRepository;
import demo.server.service.branch.BranchScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionBillingPolicyService {

    private final SessionBillingPolicyRepository policyRepository;
    private final BranchRepository branchRepository;
    private final ZoneRepository zoneRepository;
    private final StationRepository stationRepository;
    private final BranchScope branchScope;
    private final AuditRecorder auditRecorder;

    public SessionBillingPolicyService(SessionBillingPolicyRepository policyRepository, BranchRepository branchRepository,
                                       ZoneRepository zoneRepository, StationRepository stationRepository,
                                       BranchScope branchScope, AuditRecorder auditRecorder) {
        this.policyRepository = policyRepository;
        this.branchRepository = branchRepository;
        this.zoneRepository = zoneRepository;
        this.stationRepository = stationRepository;
        this.branchScope = branchScope;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public SessionBillingPolicyResponse create(SessionBillingPolicyRequest request) {
        SessionBillingPolicy policy = new SessionBillingPolicy();
        apply(policy, request);
        SessionBillingPolicy saved = policyRepository.save(policy);
        auditRecorder.record(AuditAction.UPDATE_SESSION_BILLING_POLICY, "SessionBillingPolicy", saved.getId(), null, toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SessionBillingPolicyResponse> list(UUID branchId) {
        UUID scopedBranchId = branchScope.requireScopedBranch(branchId);
        return policyRepository.findByBranch_IdAndDeletedFalseOrderByCreatedAtDesc(scopedBranchId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SessionBillingPolicyResponse update(UUID id, SessionBillingPolicyRequest request) {
        SessionBillingPolicy policy = policyRepository.findById(id).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Session billing policy not found"));
        SessionBillingPolicyResponse before = toResponse(policy);
        apply(policy, request);
        auditRecorder.record(AuditAction.UPDATE_SESSION_BILLING_POLICY, "SessionBillingPolicy", policy.getId(), before, toResponse(policy));
        return toResponse(policy);
    }

    @Transactional(readOnly = true)
    public BigDecimal estimate(PlaySession session) {
        long minutes = Math.max(1, ChronoUnit.MINUTES.between(session.getStartedAt(), session.getEndedAt() == null
                ? java.time.Instant.now()
                : session.getEndedAt()));
        return calculate(session.getStation(), minutes);
    }

    @Transactional(readOnly = true)
    public BigDecimal finalCost(PlaySession session) {
        long minutes = Math.max(1, ChronoUnit.MINUTES.between(session.getStartedAt(), session.getEndedAt()));
        return calculate(session.getStation(), minutes);
    }

    @Transactional(readOnly = true)
    public BigDecimal finalCostOrZero(PlaySession session) {
        long minutes = Math.max(1, ChronoUnit.MINUTES.between(session.getStartedAt(), session.getEndedAt()));
        return resolveOptional(session.getStation())
                .map(policy -> calculate(policy, minutes))
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal calculate(Station station, long rawMinutes) {
        return calculate(resolve(station), rawMinutes);
    }

    private BigDecimal calculate(SessionBillingPolicy policy, long rawMinutes) {
        long increment = policy.getBillingIncrementMinutes();
        long billedMinutes = ((rawMinutes + increment - 1) / increment) * increment;
        BigDecimal cost = policy.getHourlyRate()
                .multiply(BigDecimal.valueOf(billedMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        if (cost.compareTo(policy.getMinimumCharge()) < 0) {
            return policy.getMinimumCharge().setScale(2, RoundingMode.HALF_UP);
        }
        return cost.setScale(2, RoundingMode.HALF_UP);
    }

    private SessionBillingPolicy resolve(Station station) {
        return resolveOptional(station)
                .orElseThrow(() -> new BusinessRuleException("No active session billing policy configured"));
    }

    private Optional<SessionBillingPolicy> resolveOptional(Station station) {
        if (station.getId() != null) {
            var stationPolicy = policyRepository.findFirstByStation_IdAndActiveTrueAndDeletedFalseOrderByCreatedAtDesc(station.getId());
            if (stationPolicy.isPresent()) {
                return stationPolicy;
            }
        }
        if (station.getZone() != null) {
            var zonePolicy = policyRepository.findFirstByZone_IdAndStationIsNullAndActiveTrueAndDeletedFalseOrderByCreatedAtDesc(station.getZone().getId());
            if (zonePolicy.isPresent()) {
                return zonePolicy;
            }
        }
        return policyRepository.findFirstByBranch_IdAndZoneIsNullAndStationIsNullAndActiveTrueAndDeletedFalseOrderByCreatedAtDesc(station.getBranch().getId());
    }

    private void apply(SessionBillingPolicy policy, SessionBillingPolicyRequest request) {
        branchScope.assertBranchAllowed(request.branchId());
        Branch branch = branchRepository.findById(request.branchId()).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        Zone zone = null;
        if (request.zoneId() != null) {
            zone = zoneRepository.findById(request.zoneId()).filter(item -> !item.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Zone not found"));
            if (!zone.getBranch().getId().equals(branch.getId())) {
                throw new BusinessRuleException("Zone must belong to policy branch");
            }
        }
        Station station = null;
        if (request.stationId() != null) {
            station = stationRepository.findById(request.stationId()).filter(item -> !item.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Station not found"));
            if (!station.getBranch().getId().equals(branch.getId())) {
                throw new BusinessRuleException("Station must belong to policy branch");
            }
            if (zone != null && (station.getZone() == null || !station.getZone().getId().equals(zone.getId()))) {
                throw new BusinessRuleException("Station must belong to policy zone");
            }
        }
        policy.setBranch(branch);
        policy.setZone(zone);
        policy.setStation(station);
        policy.setHourlyRate(request.hourlyRate().setScale(2, RoundingMode.HALF_UP));
        policy.setMinimumCharge(request.minimumCharge().setScale(2, RoundingMode.HALF_UP));
        policy.setBillingIncrementMinutes(request.billingIncrementMinutes());
        policy.setActive(request.active() == null || request.active());
    }

    private SessionBillingPolicyResponse toResponse(SessionBillingPolicy policy) {
        return new SessionBillingPolicyResponse(policy.getId(), policy.getBranch().getId(),
                policy.getZone() == null ? null : policy.getZone().getId(),
                policy.getStation() == null ? null : policy.getStation().getId(),
                policy.getHourlyRate(), policy.getMinimumCharge(), policy.getBillingIncrementMinutes(), policy.isActive());
    }
}
