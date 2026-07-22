package demo.server.service.notification;

import demo.server.common.audit.AuditRecorder;
import demo.server.common.enums.AuditAction;
import demo.server.common.enums.NotificationChannel;
import demo.server.common.enums.NotificationDeliveryStatus;
import demo.server.common.enums.NotificationType;
import demo.server.common.response.PageResponse;
import demo.server.common.security.CurrentUserProvider;
import demo.server.common.security.TokenHashService;
import demo.server.common.time.ClockProvider;
import demo.server.common.websocket.WebSocketEventPublisher;
import demo.server.common.websocket.WebSocketTopics;
import demo.server.dto.notification.NotificationDeliveryResponse;
import demo.server.dto.notification.NotificationResponse;
import demo.server.dto.notification.PushSubscriptionRequest;
import demo.server.dto.notification.PushSubscriptionResponse;
import demo.server.entity.auth.AppUser;
import demo.server.entity.notification.Notification;
import demo.server.entity.notification.NotificationDelivery;
import demo.server.entity.notification.PushSubscription;
import demo.server.exception.ForbiddenException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.exception.UnauthorizedException;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.notification.NotificationDeliveryRepository;
import demo.server.repository.notification.NotificationRepository;
import demo.server.repository.notification.PushSubscriptionRepository;
import demo.server.service.branch.BranchScope;
import demo.server.service.notification.delivery.BrowserPushSender;
import demo.server.service.notification.delivery.DeliverySendResult;
import demo.server.service.notification.delivery.EmailSender;
import demo.server.service.social.SocialGuard;
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
public class NotificationService {

    private static final int DELIVERY_MAX_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofMinutes(5);

    private final NotificationRepository notificationRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final AppUserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TokenHashService tokenHashService;
    private final ClockProvider clockProvider;
    private final BranchScope branchScope;
    private final SocialGuard socialGuard;
    private final WebSocketEventPublisher webSocketEventPublisher;
    private final BrowserPushSender browserPushSender;
    private final EmailSender emailSender;
    private final AuditRecorder auditRecorder;

    public NotificationService(NotificationRepository notificationRepository, PushSubscriptionRepository pushSubscriptionRepository,
                               NotificationDeliveryRepository deliveryRepository, AppUserRepository userRepository,
                               CurrentUserProvider currentUserProvider, TokenHashService tokenHashService,
                               ClockProvider clockProvider, BranchScope branchScope, SocialGuard socialGuard,
                               WebSocketEventPublisher webSocketEventPublisher, BrowserPushSender browserPushSender,
                               EmailSender emailSender, AuditRecorder auditRecorder) {
        this.notificationRepository = notificationRepository;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.deliveryRepository = deliveryRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.tokenHashService = tokenHashService;
        this.clockProvider = clockProvider;
        this.branchScope = branchScope;
        this.socialGuard = socialGuard;
        this.webSocketEventPublisher = webSocketEventPublisher;
        this.browserPushSender = browserPushSender;
        this.emailSender = emailSender;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public void social(UUID senderId, UUID receiverId, NotificationType type, String title, String content,
                       String entityType, UUID entityId) {
        if (!socialGuard.canSendSocialNotification(senderId, receiverId)) {
            return;
        }
        AppUser user = user(receiverId);
        create(user, type, title, content, entityType, entityId == null ? null : entityId.toString(), branchId(user), false);
    }

    @Transactional
    public NotificationResponse accountSecurity(UUID userId, String title, String content) {
        AppUser user = user(userId);
        if (user.getBranch() != null) {
            branchScope.assertBranchAllowed(user.getBranch().getId());
        } else {
            branchScope.requireSuperAdmin();
        }
        Notification notification = create(user, NotificationType.ACCOUNT_SECURITY, title, content, "AppUser", userId.toString(), branchId(user), true);
        return toNotification(notification);
    }

    @Transactional
    public boolean sessionEndingWarning(UUID userId, UUID sessionId, String title, String content) {
        if (notificationRepository.existsByUserIdAndTypeAndEntityTypeAndEntityIdAndDeletedFalse(
                userId, NotificationType.SESSION_ENDING, "PlaySession", sessionId.toString())) {
            return false;
        }
        AppUser user = user(userId);
        create(user, NotificationType.SESSION_ENDING, title, content, "PlaySession", sessionId.toString(), branchId(user), false);
        return true;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> myNotifications(int page, int size) {
        UUID userId = currentUserId();
        return PageResponse.from(notificationRepository
                .findByUserIdAndHiddenAtIsNullAndDeletedFalseOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)))
                .map(this::toNotification));
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByUserIdAndReadAtIsNullAndHiddenAtIsNullAndDeletedFalse(currentUserId());
    }

    @Transactional
    public NotificationResponse markRead(UUID id) {
        Notification notification = ownedNotification(id);
        if (notification.getReadAt() == null) {
            notification.setReadAt(clockProvider.now());
            auditRecorder.record(AuditAction.READ_NOTIFICATION, "Notification", id, null, Map.of("readAt", notification.getReadAt()));
        }
        return toNotification(notification);
    }

    @Transactional
    public int markAllRead() {
        int updated = notificationRepository.markAllRead(currentUserId(), clockProvider.now());
        auditRecorder.record(AuditAction.READ_NOTIFICATION, "Notification", currentUserId(), null, Map.of("readAll", updated));
        return updated;
    }

    @Transactional
    public void hide(UUID id) {
        Notification notification = ownedNotification(id);
        notification.setHiddenAt(clockProvider.now());
        notification.softDelete();
        auditRecorder.record(AuditAction.DELETE_NOTIFICATION, "Notification", id, null, Map.of("hidden", true));
    }

    @Transactional
    public PushSubscriptionResponse subscribe(PushSubscriptionRequest request) {
        AppUser user = user(currentUserId());
        String endpointHash = tokenHashService.hash(request.endpoint());
        PushSubscription subscription = pushSubscriptionRepository.findByEndpointHash(endpointHash).orElseGet(PushSubscription::new);
        if (subscription.getUser() != null && !subscription.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Push subscription belongs to another user");
        }
        subscription.setUser(user);
        subscription.setEndpointHash(endpointHash);
        subscription.setEndpoint(request.endpoint());
        subscription.setP256dh(request.p256dh());
        subscription.setAuth(request.auth());
        subscription.setUserAgent(request.userAgent());
        subscription.setLastUsedAt(clockProvider.now());
        subscription.setDeleted(false);
        subscription.setDeletedAt(null);
        PushSubscription saved = pushSubscriptionRepository.save(subscription);
        auditRecorder.record(AuditAction.UPDATE_PUSH_SUBSCRIPTION, "PushSubscription", saved.getId(), null, toPushSubscription(saved));
        return toPushSubscription(saved);
    }

    @Transactional
    public void unsubscribe(UUID id) {
        PushSubscription subscription = pushSubscriptionRepository.findById(id).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Push subscription not found"));
        if (!subscription.getUser().getId().equals(currentUserId())) {
            throw new ResourceNotFoundException("Push subscription not found");
        }
        subscription.softDelete();
        auditRecorder.record(AuditAction.UPDATE_PUSH_SUBSCRIPTION, "PushSubscription", id, null, Map.of("deleted", true));
    }

    @Transactional(readOnly = true)
    public List<NotificationDeliveryResponse> deliveries(UUID notificationId) {
        Notification notification = ownedNotification(notificationId);
        return deliveryRepository.findByNotificationIdOrderByCreatedAtAsc(notification.getId()).stream().map(this::toDelivery).toList();
    }

    @Transactional
    public List<NotificationDeliveryResponse> retryDueDeliveries() {
        return deliveryRepository.findByStatusAndAttemptCountLessThanAndNextAttemptAtBefore(NotificationDeliveryStatus.FAILED, DELIVERY_MAX_ATTEMPTS, clockProvider.now())
                .stream()
                .map(this::attemptDelivery)
                .map(this::toDelivery)
                .toList();
    }

    private Notification create(AppUser user, NotificationType type, String title, String content, String entityType,
                                String entityId, String branchId, boolean emailAccountSecurity) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setChannel(NotificationChannel.IN_APP);
        notification.setTitle(mask(title));
        notification.setContent(mask(content));
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notification.setBranchId(branchId);
        Notification saved = notificationRepository.save(notification);
        createDelivery(saved, NotificationChannel.WEBSOCKET, null);
        pushSubscriptionRepository.findByUserIdAndDeletedFalse(user.getId())
                .forEach(subscription -> createDelivery(saved, NotificationChannel.WEB_PUSH, subscription));
        if (emailAccountSecurity) {
            createDelivery(saved, NotificationChannel.EMAIL, null);
        }
        webSocketEventPublisher.sendAfterCommit(WebSocketTopics.user(user.getId()), type.name(), 1,
                Map.of("notificationId", saved.getId().toString(), "entityType", nullToEmpty(entityType), "entityId", nullToEmpty(entityId)));
        auditRecorder.record(AuditAction.CREATE_NOTIFICATION, "Notification", saved.getId(), null, toNotification(saved));
        deliveryRepository.findByNotificationIdOrderByCreatedAtAsc(saved.getId()).forEach(this::attemptDelivery);
        return saved;
    }

    private NotificationDelivery createDelivery(Notification notification, NotificationChannel channel, PushSubscription pushSubscription) {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setNotification(notification);
        delivery.setChannel(channel);
        delivery.setPushSubscription(pushSubscription);
        delivery.setStatus(NotificationDeliveryStatus.PENDING);
        delivery.setMaxAttempts(DELIVERY_MAX_ATTEMPTS);
        return deliveryRepository.save(delivery);
    }

    private NotificationDelivery attemptDelivery(NotificationDelivery delivery) {
        if (delivery.getStatus() == NotificationDeliveryStatus.SENT || delivery.getAttemptCount() >= delivery.getMaxAttempts()) {
            return delivery;
        }
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setLastAttemptAt(clockProvider.now());
        DeliverySendResult result;
        if (delivery.getChannel() == NotificationChannel.WEBSOCKET || delivery.getChannel() == NotificationChannel.IN_APP) {
            result = DeliverySendResult.sent("In-app/WebSocket delivery recorded");
        } else if (delivery.getChannel() == NotificationChannel.WEB_PUSH || delivery.getChannel() == NotificationChannel.BROWSER) {
            if (delivery.getPushSubscription() == null || delivery.getPushSubscription().isDeleted()) {
                result = DeliverySendResult.failed("Push subscription is unavailable");
            } else {
                result = browserPushSender.send(delivery.getNotification(), delivery.getPushSubscription());
            }
        } else if (delivery.getChannel() == NotificationChannel.EMAIL) {
            result = emailSender.sendAccountSecurityEmail(maskEmail(delivery.getNotification().getUser().getEmail()), delivery.getNotification());
        } else {
            result = DeliverySendResult.failed("Unsupported delivery channel");
        }
        delivery.setStatus(result.sent() ? NotificationDeliveryStatus.SENT : NotificationDeliveryStatus.FAILED);
        delivery.setFailureReason(result.sent() ? null : result.message());
        delivery.setNextAttemptAt(result.sent() ? null : clockProvider.now().plus(RETRY_DELAY));
        auditRecorder.record(AuditAction.NOTIFICATION_DELIVERY, "NotificationDelivery", delivery.getId(), null, toDelivery(delivery));
        return delivery;
    }

    private Notification ownedNotification(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .filter(item -> !item.isDeleted() && item.getHiddenAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUser().getId().equals(currentUserId())) {
            throw new ResourceNotFoundException("Notification not found");
        }
        return notification;
    }

    private AppUser user(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UUID currentUserId() {
        return currentUserProvider.currentUserId().orElseThrow(() -> new UnauthorizedException("Authentication is required"));
    }

    private String branchId(AppUser user) {
        return user.getBranch() == null ? null : user.getBranch().getId().toString();
    }

    private NotificationResponse toNotification(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getChannel(),
                notification.getTitle(), notification.getContent(), notification.getReadAt(), notification.getEntityType(),
                notification.getEntityId(), notification.getBranchId(), notification.getCreatedAt());
    }

    private PushSubscriptionResponse toPushSubscription(PushSubscription subscription) {
        return new PushSubscriptionResponse(subscription.getId(), subscription.getUser().getId(), maskEndpoint(subscription.getEndpoint()),
                subscription.getUserAgent(), subscription.getLastUsedAt(), subscription.isDeleted());
    }

    private NotificationDeliveryResponse toDelivery(NotificationDelivery delivery) {
        return new NotificationDeliveryResponse(delivery.getId(), delivery.getNotification().getId(),
                delivery.getPushSubscription() == null ? null : delivery.getPushSubscription().getId(), delivery.getChannel(),
                delivery.getStatus(), delivery.getAttemptCount(), delivery.getMaxAttempts(), delivery.getLastAttemptAt(),
                delivery.getNextAttemptAt(), delivery.getFailureReason());
    }

    private String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return maskEmail(value).replaceAll("\\b\\d{8,15}\\b", "***");
    }

    private String maskEmail(String value) {
        if (!StringUtils.hasText(value) || !value.contains("@")) {
            return value;
        }
        int at = value.indexOf('@');
        String local = value.substring(0, at);
        return (local.length() <= 2 ? "***" : local.charAt(0) + "***" + local.charAt(local.length() - 1)) + value.substring(at);
    }

    private String maskEndpoint(String endpoint) {
        if (endpoint == null || endpoint.length() <= 20) {
            return "***";
        }
        return endpoint.substring(0, 12) + "***" + endpoint.substring(endpoint.length() - 8);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
