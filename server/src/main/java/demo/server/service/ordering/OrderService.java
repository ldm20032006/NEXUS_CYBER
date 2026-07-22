package demo.server.service.ordering;

import demo.server.common.audit.AuditRecorder;
import demo.server.common.enums.AuditAction;
import demo.server.common.enums.MenuItemStatus;
import demo.server.common.enums.OrderStatus;
import demo.server.common.enums.PaymentMethod;
import demo.server.common.enums.RoleCode;
import demo.server.common.enums.SessionStatus;
import demo.server.common.event.DomainEventEnvelopeFactory;
import demo.server.common.event.DomainEventPublisher;
import demo.server.common.resilience.DistributedLockService;
import demo.server.common.resilience.IdempotencyDecision;
import demo.server.common.resilience.IdempotencyDecisionType;
import demo.server.common.resilience.IdempotencyService;
import demo.server.common.resilience.LockHandle;
import demo.server.common.resilience.ResilienceKeys;
import demo.server.common.security.AuthenticatedUser;
import demo.server.common.security.CurrentUserProvider;
import demo.server.common.websocket.WebSocketEventPublisher;
import demo.server.common.websocket.WebSocketTopics;
import demo.server.dto.ordering.CancelOrderRequest;
import demo.server.dto.ordering.CreateOrderItemRequest;
import demo.server.dto.ordering.CreateOrderRequest;
import demo.server.dto.ordering.FoodOrderResponse;
import demo.server.entity.auth.AppUser;
import demo.server.entity.ordering.FoodOrder;
import demo.server.entity.ordering.MenuItem;
import demo.server.entity.ordering.OrderItem;
import demo.server.entity.session.PlaySession;
import demo.server.entity.wallet.WalletTransaction;
import demo.server.exception.BusinessRuleException;
import demo.server.exception.ConcurrencyConflictException;
import demo.server.exception.ForbiddenException;
import demo.server.exception.InvalidTransitionException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.exception.UnauthorizedException;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.ordering.FoodOrderRepository;
import demo.server.repository.ordering.MenuItemRepository;
import demo.server.repository.ordering.OrderItemRepository;
import demo.server.repository.session.PlaySessionRepository;
import demo.server.service.branch.BranchScope;
import demo.server.service.wallet.WalletService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final FoodOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final PlaySessionRepository sessionRepository;
    private final AppUserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final BranchScope branchScope;
    private final DistributedLockService lockService;
    private final IdempotencyService idempotencyService;
    private final ResilienceKeys resilienceKeys;
    private final WalletService walletService;
    private final OrderMapper mapper;
    private final AuditRecorder auditRecorder;
    private final DomainEventPublisher domainEventPublisher;
    private final DomainEventEnvelopeFactory envelopeFactory;
    private final WebSocketEventPublisher webSocketEventPublisher;

    public OrderService(FoodOrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        MenuItemRepository menuItemRepository, PlaySessionRepository sessionRepository,
                        AppUserRepository userRepository, CurrentUserProvider currentUserProvider,
                        BranchScope branchScope, DistributedLockService lockService, IdempotencyService idempotencyService,
                        ResilienceKeys resilienceKeys, WalletService walletService, OrderMapper mapper,
                        AuditRecorder auditRecorder, DomainEventPublisher domainEventPublisher,
                        DomainEventEnvelopeFactory envelopeFactory, WebSocketEventPublisher webSocketEventPublisher) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.branchScope = branchScope;
        this.lockService = lockService;
        this.idempotencyService = idempotencyService;
        this.resilienceKeys = resilienceKeys;
        this.walletService = walletService;
        this.mapper = mapper;
        this.auditRecorder = auditRecorder;
        this.domainEventPublisher = domainEventPublisher;
        this.envelopeFactory = envelopeFactory;
        this.webSocketEventPublisher = webSocketEventPublisher;
    }

    @Transactional
    public FoodOrderResponse create(CreateOrderRequest request, String idempotencyKey) {
        UUID userId = currentUserProvider.currentUserId().orElseThrow(() -> new UnauthorizedException("Authentication is required"));
        String key = idempotencyKey("create-order", idempotencyKey);
        String fingerprint = userId + ":" + request.paymentMethod() + ":" + request.items();
        if (key != null) {
            IdempotencyDecision decision = idempotencyService.begin(key, fingerprint, IDEMPOTENCY_TTL);
            if (decision.type() == IdempotencyDecisionType.REPLAY) {
                return orderRepository.findByIdempotencyKey(key).map(mapper::toOrder)
                        .orElseThrow(() -> new ConcurrencyConflictException("Idempotent order result is not available"));
            }
            if (decision.type() != IdempotencyDecisionType.STARTED) {
                throw new ConcurrencyConflictException("Request with this Idempotency-Key cannot proceed");
            }
        }
        try {
            PlaySession session = activeSession(userId);
            PaymentMethod paymentMethod = resolvePaymentMethod(session, request.paymentMethod());
            AppUser user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
            FoodOrder order = new FoodOrder();
            order.setUser(user);
            order.setBranch(session.getStation().getBranch());
            order.setStation(session.getStation());
            order.setPlaySession(session);
            order.setPaymentMethod(paymentMethod);
            order.setStatus(OrderStatus.NEW);
            order.setNote(request.note());
            order.setIdempotencyKey(key);
            FoodOrder saved = orderRepository.save(order);
            BigDecimal total = reserveStockAndItems(saved, request.items());
            saved.setTotalAmount(total);
            if (paymentMethod == PaymentMethod.WALLET) {
                WalletTransaction payment = walletService.payOrder(userId, saved.getId(), total, "order:" + saved.getId());
                saved.setPaymentWalletTransaction(payment);
            }
            auditRecorder.record(AuditAction.ORDER_STATUS_CHANGE, "FoodOrder", saved.getId(), null, mapper.toOrder(saved));
            publish(saved, "ORDER_CREATED");
            if (key != null) {
                idempotencyService.complete(key, 200);
            }
            return mapper.toOrder(saved);
        } catch (RuntimeException ex) {
            if (key != null) {
                idempotencyService.fail(key, 409);
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<FoodOrderResponse> myOrders() {
        UUID userId = currentUserProvider.currentUserId().orElseThrow(() -> new UnauthorizedException("Authentication is required"));
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(mapper::toOrder).toList();
    }

    @Transactional(readOnly = true)
    public FoodOrderResponse get(UUID id) {
        FoodOrder order = order(id);
        AuthenticatedUser actor = actorOrNull();
        UUID userId = currentUserProvider.currentUserId().orElse(null);
        if (actor == null || (!order.getUser().getId().equals(userId) && !branchScope.isSuperAdmin(actor)
                && (actor.branchId() == null || !actor.branchId().equals(order.getBranch().getId())))) {
            throw new ForbiddenException("Order is outside current scope");
        }
        return mapper.toOrder(order);
    }

    @Transactional(readOnly = true)
    public List<FoodOrderResponse> staffOrders(OrderStatus status) {
        UUID branchId = branchScope.requireScopedBranch(null);
        List<FoodOrder> orders = status == null
                ? orderRepository.findByBranchIdOrderByCreatedAtDesc(branchId)
                : orderRepository.findByBranchIdAndStatusOrderByCreatedAtAsc(branchId, status);
        return orders.stream().map(mapper::toOrder).toList();
    }

    @Transactional
    public FoodOrderResponse updateStatus(UUID id, OrderStatus next) {
        FoodOrder order = order(id);
        branchScope.assertBranchAllowed(order.getBranch().getId());
        transition(order, next);
        auditRecorder.record(AuditAction.ORDER_STATUS_CHANGE, "FoodOrder", order.getId(), null, mapper.toOrder(order));
        publish(order, "ORDER_STATUS_CHANGED");
        return mapper.toOrder(order);
    }

    @Transactional
    public FoodOrderResponse cancel(UUID id, CancelOrderRequest request) {
        FoodOrder order = order(id);
        assertCancelAllowed(order);
        if (order.getStatus() != OrderStatus.NEW && order.getStatus() != OrderStatus.ACCEPTED) {
            throw new InvalidTransitionException("Order cannot be cancelled from current status");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(request.reason());
        order.setCancelledAt(Instant.now());
        restoreStock(order);
        if (order.getPaymentMethod() == PaymentMethod.WALLET && order.getPaymentWalletTransaction() != null) {
            WalletTransaction refund = walletService.refundOrder(order.getUser().getId(), order.getId(),
                    order.getPaymentWalletTransaction(), request.reason(), "order-refund:" + order.getId());
            order.setRefundWalletTransaction(refund);
        }
        auditRecorder.record(AuditAction.ORDER_CANCELLATION, "FoodOrder", order.getId(), null, mapper.toOrder(order));
        publish(order, "ORDER_CANCELLED");
        return mapper.toOrder(order);
    }

    private BigDecimal reserveStockAndItems(FoodOrder order, List<CreateOrderItemRequest> requests) {
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        List<CreateOrderItemRequest> sorted = requests.stream()
                .sorted(Comparator.comparing(item -> item.menuItemId().toString()))
                .toList();
        for (CreateOrderItemRequest request : sorted) {
            try (LockHandle ignored = lockService.tryAcquire(resilienceKeys.lockStock(request.menuItemId()), LOCK_TTL)
                    .orElseThrow(() -> new ConcurrencyConflictException("Menu item stock is locked"))) {
                MenuItem item = menuItemRepository.findByIdForUpdate(request.menuItemId())
                        .filter(menuItem -> !menuItem.isDeleted())
                        .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
                if (!item.getBranch().getId().equals(order.getBranch().getId())) {
                    throw new BusinessRuleException("Menu item is outside active session branch");
                }
                if (item.getStatus() != MenuItemStatus.ACTIVE) {
                    throw new BusinessRuleException("Menu item is not available");
                }
                if (item.getStockQuantity() == null || item.getStockQuantity() < request.quantity()) {
                    throw new BusinessRuleException("Menu item stock is insufficient");
                }
                item.setStockQuantity(item.getStockQuantity() - request.quantity());
                if (item.getStockQuantity() == 0) {
                    item.setStatus(MenuItemStatus.OUT_OF_STOCK);
                }
                BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(request.quantity())).setScale(2, RoundingMode.HALF_UP);
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setMenuItem(item);
                orderItem.setItemNameSnapshot(item.getName());
                orderItem.setUnitPrice(item.getPrice());
                orderItem.setQuantity(request.quantity());
                orderItem.setLineTotal(lineTotal);
                orderItem.setNote(request.note());
                orderItemRepository.save(orderItem);
                total = total.add(lineTotal).setScale(2, RoundingMode.HALF_UP);
            }
        }
        return total;
    }

    private void restoreStock(FoodOrder order) {
        for (OrderItem orderItem : orderItemRepository.findByOrderId(order.getId())) {
            try (LockHandle ignored = lockService.tryAcquire(resilienceKeys.lockStock(orderItem.getMenuItem().getId()), LOCK_TTL)
                    .orElseThrow(() -> new ConcurrencyConflictException("Menu item stock is locked"))) {
                MenuItem item = menuItemRepository.findByIdForUpdate(orderItem.getMenuItem().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
                item.setStockQuantity((item.getStockQuantity() == null ? 0 : item.getStockQuantity()) + orderItem.getQuantity());
                if (item.getStatus() == MenuItemStatus.OUT_OF_STOCK) {
                    item.setStatus(MenuItemStatus.ACTIVE);
                }
            }
        }
    }

    private void transition(FoodOrder order, OrderStatus next) {
        if (next == OrderStatus.ACCEPTED && order.getStatus() == OrderStatus.NEW) {
            order.setStatus(next);
            order.setAcceptedAt(Instant.now());
            return;
        }
        if (next == OrderStatus.PREPARING && order.getStatus() == OrderStatus.ACCEPTED) {
            order.setStatus(next);
            order.setPreparingAt(Instant.now());
            return;
        }
        if (next == OrderStatus.READY && order.getStatus() == OrderStatus.PREPARING) {
            order.setStatus(next);
            order.setReadyAt(Instant.now());
            return;
        }
        if (next == OrderStatus.DELIVERED && order.getStatus() == OrderStatus.READY) {
            order.setStatus(next);
            order.setDeliveredAt(Instant.now());
            return;
        }
        throw new InvalidTransitionException("Invalid order status transition");
    }

    private void assertCancelAllowed(FoodOrder order) {
        AuthenticatedUser actor = actorOrNull();
        UUID userId = currentUserProvider.currentUserId().orElse(null);
        if (actor != null && order.getUser().getId().equals(userId)) {
            return;
        }
        branchScope.assertBranchAllowed(order.getBranch().getId());
    }

    private PlaySession activeSession(UUID userId) {
        return sessionRepository.findFirstByUser_IdAndStatusOrderByStartedAtDesc(userId, SessionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleException("Active session is required to create order"));
    }

    private PaymentMethod resolvePaymentMethod(PlaySession session, PaymentMethod requested) {
        String policy = session.getStation().getBranch().getPaymentPolicy();
        PaymentMethod normalized = requested == PaymentMethod.COUNTER ? PaymentMethod.PAY_AT_COUNTER : requested;
        if (!StringUtils.hasText(policy) || "PREPAID_OR_WALLET".equalsIgnoreCase(policy)) {
            return normalized == null ? PaymentMethod.WALLET : normalized;
        }
        if ("PAY_AT_COUNTER".equalsIgnoreCase(policy)) {
            if (normalized != null && normalized != PaymentMethod.PAY_AT_COUNTER) {
                throw new BusinessRuleException("Branch requires pay at counter");
            }
            return PaymentMethod.PAY_AT_COUNTER;
        }
        if ("WALLET".equalsIgnoreCase(policy) || "WALLET_REQUIRED".equalsIgnoreCase(policy)) {
            if (normalized != null && normalized != PaymentMethod.WALLET) {
                throw new BusinessRuleException("Branch requires wallet payment");
            }
            return PaymentMethod.WALLET;
        }
        return PaymentMethod.WALLET;
    }

    private FoodOrder order(UUID id) {
        return orderRepository.findById(id).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private AuthenticatedUser actorOrNull() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        return null;
    }

    private void publish(FoodOrder order, String eventType) {
        FoodOrderResponse response = mapper.toOrder(order);
        domainEventPublisher.publishAfterCommit(envelopeFactory.create(eventType, 1, Map.of("orderId", order.getId().toString())));
        webSocketEventPublisher.sendAfterCommit(WebSocketTopics.branchOrders(order.getBranch().getId()), eventType, 1, response);
        webSocketEventPublisher.sendAfterCommit(WebSocketTopics.user(order.getUser().getId()), eventType, 1, response);
    }

    private String idempotencyKey(String action, String rawKey) {
        return StringUtils.hasText(rawKey) ? resilienceKeys.idempotency(action, rawKey.trim()) : null;
    }
}
