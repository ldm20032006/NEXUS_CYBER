package demo.server.service.social;

import demo.server.common.audit.AuditRecorder;
import demo.server.common.enums.AuditAction;
import demo.server.common.enums.ModerationActionType;
import demo.server.common.enums.UserReportStatus;
import demo.server.common.enums.UserStatus;
import demo.server.common.resilience.RateLimitDecision;
import demo.server.common.resilience.RateLimitService;
import demo.server.common.resilience.ResilienceKeys;
import demo.server.common.security.CurrentUserProvider;
import demo.server.dto.gamer.PublicGamerProfileResponse;
import demo.server.dto.social.BlockUserRequest;
import demo.server.dto.social.ModerationActionRequest;
import demo.server.dto.social.ReportUserRequest;
import demo.server.dto.social.UserBlockResponse;
import demo.server.dto.social.UserReportResponse;
import demo.server.entity.auth.AppUser;
import demo.server.entity.social.UserBlock;
import demo.server.entity.social.UserReport;
import demo.server.exception.BusinessRuleException;
import demo.server.exception.ForbiddenException;
import demo.server.exception.RateLimitExceededException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.exception.UnauthorizedException;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.social.UserBlockRepository;
import demo.server.repository.social.UserReportRepository;
import demo.server.service.branch.BranchScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class SocialModerationService implements SocialGuard {

    private static final int REPORT_PERMITS = 5;
    private static final Duration REPORT_WINDOW = Duration.ofMinutes(10);

    private final UserBlockRepository blockRepository;
    private final UserReportRepository reportRepository;
    private final AppUserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final RateLimitService rateLimitService;
    private final ResilienceKeys resilienceKeys;
    private final BranchScope branchScope;
    private final SocialMapper mapper;
    private final AuditRecorder auditRecorder;

    public SocialModerationService(UserBlockRepository blockRepository, UserReportRepository reportRepository,
                                   AppUserRepository userRepository, CurrentUserProvider currentUserProvider,
                                   RateLimitService rateLimitService, ResilienceKeys resilienceKeys,
                                   BranchScope branchScope, SocialMapper mapper, AuditRecorder auditRecorder) {
        this.blockRepository = blockRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.rateLimitService = rateLimitService;
        this.resilienceKeys = resilienceKeys;
        this.branchScope = branchScope;
        this.mapper = mapper;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public UserBlockResponse block(UUID blockedUserId, BlockUserRequest request) {
        UUID blockerId = currentUserId();
        if (blockerId.equals(blockedUserId)) {
            throw new BusinessRuleException("Cannot block yourself");
        }
        AppUser blocker = user(blockerId);
        AppUser blocked = user(blockedUserId);
        UserBlock block = blockRepository.findByBlocker_IdAndBlockedUser_Id(blockerId, blockedUserId)
                .orElseGet(UserBlock::new);
        block.setBlocker(blocker);
        block.setBlockedUser(blocked);
        block.setReason(request == null ? null : request.reason());
        block.setBlockedAt(Instant.now());
        block.setDeleted(false);
        block.setDeletedAt(null);
        UserBlock saved = blockRepository.save(block);
        auditRecorder.record(AuditAction.BLOCK_USER, "UserBlock", saved.getId(), null, mapper.toBlock(saved));
        return mapper.toBlock(saved);
    }

    @Transactional
    public void unblock(UUID blockedUserId) {
        UUID blockerId = currentUserId();
        UserBlock block = blockRepository.findByBlocker_IdAndBlockedUser_IdAndDeletedFalse(blockerId, blockedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User block not found"));
        UserBlockResponse before = mapper.toBlock(block);
        block.softDelete();
        auditRecorder.record(AuditAction.UNBLOCK_USER, "UserBlock", block.getId(), before, "SOFT_DELETED");
    }

    @Transactional(readOnly = true)
    public List<UserBlockResponse> myBlocks() {
        UUID blockerId = currentUserId();
        return blockRepository.findByBlocker_IdAndDeletedFalse(blockerId).stream().map(mapper::toBlock).toList();
    }

    @Transactional
    public UserReportResponse report(UUID reportedUserId, ReportUserRequest request) {
        UUID reporterId = currentUserId();
        if (reporterId.equals(reportedUserId)) {
            throw new BusinessRuleException("Cannot report yourself");
        }
        RateLimitDecision decision = rateLimitService.consume(resilienceKeys.rateLimit("report", reporterId.toString()),
                REPORT_PERMITS, REPORT_WINDOW);
        if (!decision.allowed()) {
            throw new RateLimitExceededException("Report rate limit exceeded");
        }
        AppUser reporter = user(reporterId);
        AppUser reported = user(reportedUserId);
        UserReport report = new UserReport();
        report.setReporter(reporter);
        report.setReportedUser(reported);
        report.setBranch(reported.getBranch());
        report.setReason(request.reason());
        report.setContext(request.context());
        report.setStatus(UserReportStatus.OPEN);
        report.setReportedAt(Instant.now());
        UserReport saved = reportRepository.save(report);
        auditRecorder.record(AuditAction.REPORT_USER, "UserReport", saved.getId(), null, mapper.toReport(saved));
        return mapper.toReport(saved);
    }

    @Transactional(readOnly = true)
    public List<UserReportResponse> myReports() {
        UUID reporterId = currentUserId();
        return reportRepository.findByReporter_IdAndDeletedFalseOrderByReportedAtDesc(reporterId).stream()
                .map(mapper::toReport)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserReportResponse> adminReports(UserReportStatus status) {
        UUID branchId = branchScope.requireScopedBranch(null);
        List<UserReport> reports = status == null
                ? reportRepository.findByBranch_IdAndDeletedFalseOrderByReportedAtDesc(branchId)
                : reportRepository.findByBranch_IdAndStatusAndDeletedFalseOrderByReportedAtDesc(branchId, status);
        return reports.stream().map(mapper::toReport).toList();
    }

    @Transactional
    public UserReportResponse moderate(UUID reportId, ModerationActionRequest request) {
        UserReport report = reportRepository.findByIdAndDeletedFalse(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("User report not found"));
        if (report.getBranch() == null) {
            branchScope.requireSuperAdmin();
        } else {
            branchScope.assertBranchAllowed(report.getBranch().getId());
        }
        UserReportResponse before = mapper.toReport(report);
        report.setStatus(request.status());
        report.setModerationAction(request.action());
        report.setModerationNote(request.note());
        report.setModerator(user(currentUserId()));
        report.setModeratedAt(Instant.now());
        if (request.action() == ModerationActionType.TEMP_LOCK || request.action() == ModerationActionType.PERMANENT_LOCK) {
            report.getReportedUser().setStatus(UserStatus.LOCKED);
            report.getReportedUser().setLockedAt(Instant.now());
            report.getReportedUser().setLockReason(request.note());
        }
        auditRecorder.record(AuditAction.MODERATION_ACTION, "UserReport", report.getId(), before, mapper.toReport(report));
        return mapper.toReport(report);
    }

    @Transactional(readOnly = true)
    public List<PublicGamerProfileResponse> radar(UUID branchId) {
        UUID viewerId = currentUserId();
        List<AppUser> candidates = userRepository.findAllByBranch_Id(branchId).stream()
                .filter(user -> !user.isDeleted() && user.getStatus() == UserStatus.ACTIVE)
                .toList();
        return filterVisibleTo(viewerId, candidates).stream().map(mapper::toPublicUser).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlockedBetween(UUID firstUserId, UUID secondUserId) {
        return blockRepository.existsByBlocker_IdAndBlockedUser_IdAndDeletedFalse(firstUserId, secondUserId)
                || blockRepository.existsByBlocker_IdAndBlockedUser_IdAndDeletedFalse(secondUserId, firstUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canSendInvitation(UUID senderId, UUID receiverId) {
        return !senderId.equals(receiverId) && !isBlockedBetween(senderId, receiverId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canSendSocialNotification(UUID senderId, UUID receiverId) {
        return canSendInvitation(senderId, receiverId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppUser> filterVisibleTo(UUID viewerId, Collection<AppUser> candidates) {
        return candidates.stream()
                .filter(candidate -> !candidate.getId().equals(viewerId))
                .filter(candidate -> !isBlockedBetween(viewerId, candidate.getId()))
                .toList();
    }

    private AppUser user(UUID userId) {
        return userRepository.findById(userId).filter(user -> !user.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UUID currentUserId() {
        return currentUserProvider.currentUserId().orElseThrow(() -> new UnauthorizedException("Authentication is required"));
    }
}
