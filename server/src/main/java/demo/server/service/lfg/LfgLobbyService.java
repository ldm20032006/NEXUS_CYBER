package demo.server.service.lfg;

import demo.server.common.audit.AuditRecorder;
import demo.server.common.enums.AuditAction;
import demo.server.common.enums.GameStatus;
import demo.server.common.enums.InvitationStatus;
import demo.server.common.enums.LfgSignalStatus;
import demo.server.common.enums.LobbyMemberRole;
import demo.server.common.enums.LobbyMemberStatus;
import demo.server.common.enums.LobbyStatus;
import demo.server.common.enums.MessageType;
import demo.server.common.enums.NotificationType;
import demo.server.common.enums.SessionStatus;
import demo.server.common.resilience.DistributedLockService;
import demo.server.common.resilience.LockHandle;
import demo.server.common.resilience.RateLimitDecision;
import demo.server.common.resilience.RateLimitService;
import demo.server.common.resilience.ResilienceKeys;
import demo.server.common.security.CurrentUserProvider;
import demo.server.common.websocket.WebSocketEventPublisher;
import demo.server.common.websocket.WebSocketTopics;
import demo.server.dto.lfg.LfgSearchRequest;
import demo.server.dto.lfg.LfgSignalRequest;
import demo.server.dto.lfg.LfgSignalResponse;
import demo.server.dto.lfg.TeamInvitationRequest;
import demo.server.dto.lfg.TeamInvitationResponse;
import demo.server.dto.lobby.CreateLobbyRequest;
import demo.server.dto.lobby.LobbyMessageRequest;
import demo.server.dto.lobby.LobbyMessageResponse;
import demo.server.dto.lobby.LobbyResponse;
import demo.server.entity.auth.AppUser;
import demo.server.entity.branch.Zone;
import demo.server.entity.game.Game;
import demo.server.entity.game.GameRank;
import demo.server.entity.game.GameRole;
import demo.server.entity.game.GamerGameProfile;
import demo.server.entity.lfg.LfgSignal;
import demo.server.entity.lfg.TeamInvitation;
import demo.server.entity.lobby.Lobby;
import demo.server.entity.lobby.LobbyMember;
import demo.server.entity.lobby.LobbyMessage;
import demo.server.entity.session.PlaySession;
import demo.server.exception.BusinessRuleException;
import demo.server.exception.ConcurrencyConflictException;
import demo.server.exception.ForbiddenException;
import demo.server.exception.InvalidTransitionException;
import demo.server.exception.RateLimitExceededException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.exception.UnauthorizedException;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.branch.ZoneRepository;
import demo.server.repository.game.GameRankRepository;
import demo.server.repository.game.GameRepository;
import demo.server.repository.game.GameRoleRepository;
import demo.server.repository.game.GamerGameProfileRepository;
import demo.server.repository.lfg.LfgSignalRepository;
import demo.server.repository.lfg.TeamInvitationRepository;
import demo.server.repository.lobby.LobbyMemberRepository;
import demo.server.repository.lobby.LobbyMessageRepository;
import demo.server.repository.lobby.LobbyRepository;
import demo.server.repository.session.PlaySessionRepository;
import demo.server.service.notification.NotificationService;
import demo.server.service.social.SocialGuard;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LfgLobbyService {

    private static final Duration LFG_TTL = Duration.ofMinutes(15);
    private static final Duration INVITATION_TTL = Duration.ofSeconds(60);
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final LfgSignalRepository lfgRepository;
    private final TeamInvitationRepository invitationRepository;
    private final LobbyRepository lobbyRepository;
    private final LobbyMemberRepository memberRepository;
    private final LobbyMessageRepository messageRepository;
    private final PlaySessionRepository sessionRepository;
    private final GamerGameProfileRepository gameProfileRepository;
    private final GameRepository gameRepository;
    private final GameRankRepository rankRepository;
    private final GameRoleRepository roleRepository;
    private final ZoneRepository zoneRepository;
    private final AppUserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final SocialGuard socialGuard;
    private final RateLimitService rateLimitService;
    private final DistributedLockService lockService;
    private final ResilienceKeys resilienceKeys;
    private final NotificationService notificationService;
    private final LfgMapper mapper;
    private final AuditRecorder auditRecorder;
    private final WebSocketEventPublisher webSocketEventPublisher;
    private final VoiceService voiceService;

    public LfgLobbyService(LfgSignalRepository lfgRepository, TeamInvitationRepository invitationRepository,
                           LobbyRepository lobbyRepository, LobbyMemberRepository memberRepository,
                           LobbyMessageRepository messageRepository, PlaySessionRepository sessionRepository,
                           GamerGameProfileRepository gameProfileRepository, GameRepository gameRepository,
                           GameRankRepository rankRepository, GameRoleRepository roleRepository,
                           ZoneRepository zoneRepository, AppUserRepository userRepository,
                           CurrentUserProvider currentUserProvider, SocialGuard socialGuard,
                           RateLimitService rateLimitService, DistributedLockService lockService,
                           ResilienceKeys resilienceKeys, NotificationService notificationService, LfgMapper mapper,
                           AuditRecorder auditRecorder, WebSocketEventPublisher webSocketEventPublisher,
                           VoiceService voiceService) {
        this.lfgRepository = lfgRepository;
        this.invitationRepository = invitationRepository;
        this.lobbyRepository = lobbyRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.sessionRepository = sessionRepository;
        this.gameProfileRepository = gameProfileRepository;
        this.gameRepository = gameRepository;
        this.rankRepository = rankRepository;
        this.roleRepository = roleRepository;
        this.zoneRepository = zoneRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.socialGuard = socialGuard;
        this.rateLimitService = rateLimitService;
        this.lockService = lockService;
        this.resilienceKeys = resilienceKeys;
        this.notificationService = notificationService;
        this.mapper = mapper;
        this.auditRecorder = auditRecorder;
        this.webSocketEventPublisher = webSocketEventPublisher;
        this.voiceService = voiceService;
    }

    @Transactional
    public LfgSignalResponse createSignal(LfgSignalRequest request) {
        UUID userId = currentUserId();
        PlaySession session = activeSession(userId);
        GamerGameProfile profile = gameProfileRepository.findByUser_IdAndGame_Id(userId, request.gameId())
                .filter(item -> !item.isDeleted())
                .orElseThrow(() -> new BusinessRuleException("Valid game profile is required for LFG"));
        Game game = game(request.gameId());
        if (!profile.getGame().getId().equals(game.getId())) {
            throw new BusinessRuleException("Game profile does not match LFG game");
        }
        lfgRepository.findFirstByUser_IdAndStatusAndDeletedFalse(userId, LfgSignalStatus.ACTIVE)
                .ifPresent(existing -> { throw new BusinessRuleException("User already has active LFG signal"); });
        LfgSignal signal = new LfgSignal();
        signal.setUser(user(userId));
        signal.setPlaySession(session);
        signal.setBranch(session.getStation().getBranch());
        signal.setZone(resolveZone(session, request.zoneId()));
        signal.setGame(game);
        signal.setRank(resolveRank(game.getId(), request.rankId(), profile));
        signal.setRole(resolveRole(game.getId(), request.roleId(), profile));
        signal.setTargetMembers(Math.min(request.targetMembers(), game.getMaxLobbySize()));
        signal.setMessage(request.message());
        signal.setStatus(LfgSignalStatus.ACTIVE);
        signal.setExpiresAt(Instant.now().plus(LFG_TTL));
        LfgSignal saved = lfgRepository.save(signal);
        auditRecorder.record(AuditAction.CREATE_LFG_SIGNAL, "LfgSignal", saved.getId(), null, mapper.toSignal(saved));
        return mapper.toSignal(saved);
    }

    @Transactional(readOnly = true)
    public List<LfgSignalResponse> search(LfgSearchRequest request) {
        UUID userId = currentUserId();
        PlaySession session = activeSession(userId);
        UUID branchId = request.branchId() == null ? session.getStation().getBranch().getId() : request.branchId();
        return lfgRepository.searchActive(branchId, request.gameId(), request.rankId(), request.roleId(), request.zoneId(), Instant.now())
                .stream()
                .filter(signal -> !signal.getUser().getId().equals(userId))
                .filter(signal -> !socialGuard.isBlockedBetween(userId, signal.getUser().getId()))
                .map(mapper::toSignal)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LfgSignalResponse> mySignals() {
        return lfgRepository.findByUser_IdAndDeletedFalseOrderByCreatedAtDesc(currentUserId()).stream().map(mapper::toSignal).toList();
    }

    @Transactional
    public LfgSignalResponse renewSignal(UUID id) {
        LfgSignal signal = ownedSignal(id);
        if (signal.getStatus() != LfgSignalStatus.ACTIVE) {
            throw new InvalidTransitionException("Only active LFG can be renewed");
        }
        signal.setExpiresAt(Instant.now().plus(LFG_TTL));
        auditRecorder.record(AuditAction.RENEW_LFG_SIGNAL, "LfgSignal", id, null, mapper.toSignal(signal));
        return mapper.toSignal(signal);
    }

    @Transactional
    public void cancelSignal(UUID id) {
        LfgSignal signal = ownedSignal(id);
        signal.setStatus(LfgSignalStatus.CANCELLED);
        signal.softDelete();
        auditRecorder.record(AuditAction.CANCEL_LFG_SIGNAL, "LfgSignal", id, mapper.toSignal(signal), "SOFT_DELETED");
    }

    @Transactional
    public TeamInvitationResponse invite(TeamInvitationRequest request) {
        UUID senderId = currentUserId();
        if (!socialGuard.canSendInvitation(senderId, request.receiverId())) {
            throw new BusinessRuleException("Invitation is blocked");
        }
        RateLimitDecision decision = rateLimitService.consume(resilienceKeys.rateLimit("invitation", senderId.toString()), 20, Duration.ofMinutes(1));
        if (!decision.allowed()) {
            throw new RateLimitExceededException("Invitation rate limit exceeded");
        }
        if (invitationRepository.existsBySender_IdAndReceiver_IdAndStatusAndDeletedFalse(senderId, request.receiverId(), InvitationStatus.PENDING)) {
            throw new BusinessRuleException("Pending invitation already exists");
        }
        Lobby lobby = request.lobbyId() == null ? null : memberLobbyForLeader(request.lobbyId(), senderId);
        TeamInvitation invitation = new TeamInvitation();
        invitation.setSender(user(senderId));
        invitation.setReceiver(user(request.receiverId()));
        invitation.setLobby(lobby);
        invitation.setMessage(request.message());
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(Instant.now().plus(INVITATION_TTL));
        TeamInvitation saved = invitationRepository.save(invitation);
        notificationService.social(senderId, request.receiverId(), NotificationType.TEAM_INVITATION,
                "Team invitation", "You have a new team invitation", "TeamInvitation", saved.getId());
        auditRecorder.record(AuditAction.SEND_TEAM_INVITATION, "TeamInvitation", saved.getId(), null, mapper.toInvitation(saved));
        return mapper.toInvitation(saved);
    }

    @Transactional(readOnly = true)
    public List<TeamInvitationResponse> receivedInvitations() {
        return invitationRepository.findByReceiver_IdAndDeletedFalseOrderByCreatedAtDesc(currentUserId()).stream().map(this::expireIfNeededView).toList();
    }

    @Transactional(readOnly = true)
    public List<TeamInvitationResponse> sentInvitations() {
        return invitationRepository.findBySender_IdAndDeletedFalseOrderByCreatedAtDesc(currentUserId()).stream().map(this::expireIfNeededView).toList();
    }

    @Transactional(noRollbackFor = InvalidTransitionException.class)
    public LobbyResponse acceptInvitation(UUID invitationId) {
        UUID userId = currentUserId();
        try (LockHandle ignored = lockService.tryAcquire(resilienceKeys.lockInvitationAccept(invitationId), LOCK_TTL)
                .orElseThrow(() -> new ConcurrencyConflictException("Invitation accept is locked"))) {
            TeamInvitation invitation = invitationRepository.findByIdForUpdate(invitationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
            if (!invitation.getReceiver().getId().equals(userId)) {
                throw new ForbiddenException("Invitation is outside current user scope");
            }
            if (invitation.getStatus() != InvitationStatus.PENDING) {
                throw new InvalidTransitionException("Invitation is not pending");
            }
            if (invitation.getExpiresAt().isBefore(Instant.now())) {
                invitation.setStatus(InvitationStatus.EXPIRED);
                throw new InvalidTransitionException("Invitation has expired");
            }
            if (socialGuard.isBlockedBetween(invitation.getSender().getId(), invitation.getReceiver().getId())) {
                throw new BusinessRuleException("Invitation is blocked");
            }
            Lobby lobby = invitation.getLobby() == null
                    ? createLobbyInternal(invitation.getSender(), "Lobby", gameProfile(invitation.getSender().getId()).getGame())
                    : lobbyRepository.findByIdForUpdate(invitation.getLobby().getId()).orElseThrow();
            ensureCapacity(lobby);
            addOrReactivateMember(lobby, invitation.getReceiver(), LobbyMemberRole.MEMBER);
            invitation.setLobby(lobby);
            invitation.setStatus(InvitationStatus.ACCEPTED);
            invitation.setRespondedAt(Instant.now());
            auditRecorder.record(AuditAction.RESPOND_TEAM_INVITATION, "TeamInvitation", invitation.getId(), null, mapper.toInvitation(invitation));
            publishLobby(lobby, "LOBBY_UPDATED");
            return mapper.toLobby(lobby);
        }
    }

    @Transactional
    public TeamInvitationResponse rejectInvitation(UUID id) {
        return respond(id, InvitationStatus.REJECTED, true);
    }

    @Transactional
    public TeamInvitationResponse cancelInvitation(UUID id) {
        return respond(id, InvitationStatus.CANCELLED, false);
    }

    @Transactional
    public LobbyResponse createLobby(CreateLobbyRequest request) {
        UUID userId = currentUserId();
        Game game = game(request.gameId());
        gameProfile(userId, game.getId());
        Lobby lobby = createLobbyInternal(user(userId), StringUtils.hasText(request.name()) ? request.name() : game.getName() + " Lobby", game);
        auditRecorder.record(AuditAction.CREATE_LOBBY, "Lobby", lobby.getId(), null, mapper.toLobby(lobby));
        publishLobby(lobby, "LOBBY_CREATED");
        return mapper.toLobby(lobby);
    }

    @Transactional(readOnly = true)
    public LobbyResponse getLobby(UUID id) {
        Lobby lobby = lobby(id);
        requireMember(lobby.getId(), currentUserId());
        return mapper.toLobby(lobby);
    }

    @Transactional
    public LobbyResponse leave(UUID lobbyId) {
        UUID userId = currentUserId();
        Lobby lobby = lobby(lobbyId);
        LobbyMember member = requireMember(lobbyId, userId);
        member.setStatus(LobbyMemberStatus.LEFT);
        member.setLeftAt(Instant.now());
        if (member.getRole() == LobbyMemberRole.LEADER) {
            transferToFirstActiveMemberOrClose(lobby);
        }
        auditRecorder.record(AuditAction.UPDATE_LOBBY, "Lobby", lobby.getId(), null, mapper.toLobby(lobby));
        publishLobby(lobby, "LOBBY_UPDATED");
        return mapper.toLobby(lobby);
    }

    @Transactional
    public LobbyResponse kick(UUID lobbyId, UUID userId) {
        Lobby lobby = lobby(lobbyId);
        requireLeader(lobby, currentUserId());
        LobbyMember member = requireMember(lobbyId, userId);
        if (member.getRole() == LobbyMemberRole.LEADER) {
            throw new BusinessRuleException("Leader cannot be kicked");
        }
        member.setStatus(LobbyMemberStatus.KICKED);
        member.setLeftAt(Instant.now());
        publishLobby(lobby, "LOBBY_UPDATED");
        return mapper.toLobby(lobby);
    }

    @Transactional
    public LobbyResponse transfer(UUID lobbyId, UUID newLeaderId) {
        Lobby lobby = lobby(lobbyId);
        requireLeader(lobby, currentUserId());
        LobbyMember oldLeader = requireMember(lobbyId, currentUserId());
        LobbyMember newLeader = requireMember(lobbyId, newLeaderId);
        oldLeader.setRole(LobbyMemberRole.MEMBER);
        newLeader.setRole(LobbyMemberRole.LEADER);
        lobby.setLeader(newLeader.getUser());
        publishLobby(lobby, "LOBBY_UPDATED");
        return mapper.toLobby(lobby);
    }

    @Transactional
    public void disband(UUID lobbyId) {
        Lobby lobby = lobby(lobbyId);
        requireLeader(lobby, currentUserId());
        lobby.setStatus(LobbyStatus.CLOSED);
        voiceService.closeChannel(lobby);
        memberRepository.findByLobby_IdAndStatus(lobbyId, LobbyMemberStatus.ACTIVE).forEach(member -> {
            member.setStatus(LobbyMemberStatus.LEFT);
            member.setLeftAt(Instant.now());
        });
        auditRecorder.record(AuditAction.UPDATE_LOBBY, "Lobby", lobbyId, null, "DISBANDED");
        publishLobby(lobby, "LOBBY_CLOSED");
    }

    @Transactional
    public LobbyMessageResponse sendMessage(UUID lobbyId, LobbyMessageRequest request) {
        Lobby lobby = lobby(lobbyId);
        UUID userId = currentUserId();
        requireMember(lobbyId, userId);
        String content = sanitize(request.content());
        LobbyMessage message = new LobbyMessage();
        message.setLobby(lobby);
        message.setSender(user(userId));
        message.setMessageType(MessageType.TEXT);
        message.setContent(content);
        message.setSentAt(Instant.now());
        LobbyMessage saved = messageRepository.save(message);
        auditRecorder.record(AuditAction.SEND_LOBBY_MESSAGE, "LobbyMessage", saved.getId(), null, mapper.toMessage(saved));
        webSocketEventPublisher.sendAfterCommit(WebSocketTopics.lobby(lobbyId), "LOBBY_MESSAGE", 1, mapper.toMessage(saved));
        return mapper.toMessage(saved);
    }

    @Transactional(readOnly = true)
    public List<LobbyMessageResponse> messages(UUID lobbyId, int page, int size) {
        requireMember(lobbyId, currentUserId());
        return messageRepository.findByLobby_IdOrderBySentAtDesc(lobbyId, PageRequest.of(page, Math.min(size, 100))).stream()
                .map(mapper::toMessage).toList();
    }

    @EventListener
    @Transactional
    public void closeLfgOnSessionEnded(SessionEndedEvent event) {
        lfgRepository.findByPlaySession_IdAndStatusAndDeletedFalse(event.sessionId(), LfgSignalStatus.ACTIVE)
                .forEach(signal -> {
                    signal.setStatus(LfgSignalStatus.CANCELLED);
                    signal.softDelete();
                });
    }

    public record SessionEndedEvent(UUID sessionId) {
    }

    private Lobby createLobbyInternal(AppUser leader, String name, Game game) {
        PlaySession session = activeSession(leader.getId());
        Lobby lobby = new Lobby();
        lobby.setCreator(leader);
        lobby.setLeader(leader);
        lobby.setGame(game);
        lobby.setBranch(session.getStation().getBranch());
        lobby.setZone(session.getStation().getZone());
        lobby.setName(name);
        lobby.setMaxMembers(game.getMaxLobbySize());
        lobby.setStatus(LobbyStatus.OPEN);
        Lobby saved = lobbyRepository.save(lobby);
        addOrReactivateMember(saved, leader, LobbyMemberRole.LEADER);
        return saved;
    }

    private TeamInvitationResponse respond(UUID id, InvitationStatus status, boolean receiverOnly) {
        UUID userId = currentUserId();
        TeamInvitation invitation = invitationRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        if (receiverOnly && !invitation.getReceiver().getId().equals(userId)) {
            throw new ForbiddenException("Invitation is outside current user scope");
        }
        if (!receiverOnly && !invitation.getSender().getId().equals(userId)) {
            throw new ForbiddenException("Invitation is outside current user scope");
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new InvalidTransitionException("Invitation is not pending");
        }
        invitation.setStatus(status);
        invitation.setRespondedAt(Instant.now());
        return mapper.toInvitation(invitation);
    }

    private void ensureCapacity(Lobby lobby) {
        if (memberRepository.countByLobby_IdAndStatus(lobby.getId(), LobbyMemberStatus.ACTIVE) >= lobby.getMaxMembers()) {
            throw new BusinessRuleException("Lobby is full");
        }
    }

    private void addOrReactivateMember(Lobby lobby, AppUser user, LobbyMemberRole role) {
        LobbyMember member = memberRepository.findByLobby_IdAndUser_Id(lobby.getId(), user.getId()).orElseGet(LobbyMember::new);
        member.setLobby(lobby);
        member.setUser(user);
        member.setRole(role);
        member.setStatus(LobbyMemberStatus.ACTIVE);
        member.setJoinedAt(member.getJoinedAt() == null ? Instant.now() : member.getJoinedAt());
        member.setLeftAt(null);
        memberRepository.save(member);
    }

    private void transferToFirstActiveMemberOrClose(Lobby lobby) {
        List<LobbyMember> members = memberRepository.findByLobby_IdAndStatus(lobby.getId(), LobbyMemberStatus.ACTIVE);
        if (members.isEmpty()) {
            lobby.setStatus(LobbyStatus.CLOSED);
            return;
        }
        LobbyMember next = members.getFirst();
        next.setRole(LobbyMemberRole.LEADER);
        lobby.setLeader(next.getUser());
    }

    private LfgSignal ownedSignal(UUID id) {
        LfgSignal signal = lfgRepository.findByIdForUpdate(id).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("LFG signal not found"));
        if (!signal.getUser().getId().equals(currentUserId())) {
            throw new ForbiddenException("LFG signal is outside current user scope");
        }
        return signal;
    }

    private Lobby memberLobbyForLeader(UUID lobbyId, UUID leaderId) {
        Lobby lobby = lobbyRepository.findByIdForUpdate(lobbyId).orElseThrow(() -> new ResourceNotFoundException("Lobby not found"));
        requireLeader(lobby, leaderId);
        return lobby;
    }

    private LobbyMember requireMember(UUID lobbyId, UUID userId) {
        return memberRepository.findByLobby_IdAndUser_IdAndStatus(lobbyId, userId, LobbyMemberStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException("Lobby is visible to members only"));
    }

    private void requireLeader(Lobby lobby, UUID userId) {
        if (!lobby.getLeader().getId().equals(userId)) {
            throw new ForbiddenException("Lobby leader role is required");
        }
    }

    private PlaySession activeSession(UUID userId) {
        return sessionRepository.findFirstByUser_IdAndStatusOrderByStartedAtDesc(userId, SessionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleException("Active session is required"));
    }

    private Game game(UUID gameId) {
        return gameRepository.findById(gameId)
                .filter(game -> !game.isDeleted() && game.getStatus() == GameStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active game not found"));
    }

    private GamerGameProfile gameProfile(UUID userId) {
        return gameProfileRepository.findByUser_Id(userId).stream().filter(profile -> !profile.isDeleted()).findFirst()
                .orElseThrow(() -> new BusinessRuleException("Valid game profile is required"));
    }

    private GamerGameProfile gameProfile(UUID userId, UUID gameId) {
        return gameProfileRepository.findByUser_IdAndGame_Id(userId, gameId).filter(profile -> !profile.isDeleted())
                .orElseThrow(() -> new BusinessRuleException("Valid game profile is required"));
    }

    private GameRank resolveRank(UUID gameId, UUID rankId, GamerGameProfile profile) {
        if (rankId == null) {
            return profile.getRank();
        }
        GameRank rank = rankRepository.findById(rankId).orElseThrow(() -> new ResourceNotFoundException("Game rank not found"));
        if (!rank.getGame().getId().equals(gameId)) {
            throw new BusinessRuleException("Rank must belong to selected game");
        }
        return rank;
    }

    private GameRole resolveRole(UUID gameId, UUID roleId, GamerGameProfile profile) {
        if (roleId == null) {
            return profile.getPreferredRole();
        }
        GameRole role = roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Game role not found"));
        if (!role.getGame().getId().equals(gameId)) {
            throw new BusinessRuleException("Role must belong to selected game");
        }
        return role;
    }

    private Zone resolveZone(PlaySession session, UUID zoneId) {
        if (zoneId == null) {
            return session.getStation().getZone();
        }
        Zone zone = zoneRepository.findById(zoneId).orElseThrow(() -> new ResourceNotFoundException("Zone not found"));
        if (!zone.getBranch().getId().equals(session.getStation().getBranch().getId())) {
            throw new BusinessRuleException("Zone must belong to active branch");
        }
        return zone;
    }

    private Lobby lobby(UUID id) {
        return lobbyRepository.findById(id).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Lobby not found"));
    }

    private AppUser user(UUID userId) {
        return userRepository.findById(userId).filter(user -> !user.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UUID currentUserId() {
        return currentUserProvider.currentUserId().orElseThrow(() -> new UnauthorizedException("Authentication is required"));
    }

    private String sanitize(String content) {
        String normalized = content.trim().replaceAll("<[^>]*>", "");
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessRuleException("Message content is required");
        }
        if (normalized.length() > 1000) {
            throw new BusinessRuleException("Message content is too long");
        }
        return normalized;
    }

    private TeamInvitationResponse expireIfNeededView(TeamInvitation invitation) {
        if (invitation.getStatus() == InvitationStatus.PENDING && invitation.getExpiresAt().isBefore(Instant.now())) {
            return new TeamInvitationResponse(invitation.getId(), invitation.getSender().getId(), invitation.getReceiver().getId(),
                    invitation.getLobby() == null ? null : invitation.getLobby().getId(), invitation.getMessage(),
                    InvitationStatus.EXPIRED, invitation.getExpiresAt(), invitation.getRespondedAt());
        }
        return mapper.toInvitation(invitation);
    }

    private void publishLobby(Lobby lobby, String eventType) {
        LobbyResponse response = mapper.toLobby(lobby);
        webSocketEventPublisher.sendAfterCommit(WebSocketTopics.lobby(lobby.getId()), eventType, 1, response);
        webSocketEventPublisher.sendAfterCommit(WebSocketTopics.branchOrders(lobby.getBranch().getId()), eventType, 1,
                Map.of("lobbyId", lobby.getId().toString()));
    }
}
