package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.common.response.PageResponse;
import demo.server.dto.notification.AccountSecurityNotificationRequest;
import demo.server.dto.notification.NotificationDeliveryResponse;
import demo.server.dto.notification.NotificationResponse;
import demo.server.dto.notification.PushSubscriptionRequest;
import demo.server.dto.notification.PushSubscriptionResponse;
import demo.server.service.notification.NotificationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public ApiResponse<PageResponse<NotificationResponse>> notifications(@RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(notificationService.myNotifications(page, size));
    }

    @GetMapping("/notifications/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount() {
        return ApiResponse.ok(Map.of("count", notificationService.unreadCount()));
    }

    @PatchMapping("/notifications/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable UUID id) {
        return ApiResponse.ok(notificationService.markRead(id), "Notification marked read");
    }

    @PatchMapping("/notifications/read-all")
    public ApiResponse<Map<String, Integer>> markAllRead() {
        return ApiResponse.ok(Map.of("updated", notificationService.markAllRead()), "Notifications marked read");
    }

    @DeleteMapping("/notifications/{id}")
    public ApiResponse<Void> hide(@PathVariable UUID id) {
        notificationService.hide(id);
        return ApiResponse.ok(null, "Notification deleted");
    }

    @PostMapping("/notifications/push-subscriptions")
    public ApiResponse<PushSubscriptionResponse> subscribe(@Valid @RequestBody PushSubscriptionRequest request) {
        return ApiResponse.ok(notificationService.subscribe(request), "Push subscription saved");
    }

    @DeleteMapping("/notifications/push-subscriptions/{id}")
    public ApiResponse<Void> unsubscribe(@PathVariable UUID id) {
        notificationService.unsubscribe(id);
        return ApiResponse.ok(null, "Push subscription removed");
    }

    @GetMapping("/notifications/{id}/deliveries")
    public ApiResponse<List<NotificationDeliveryResponse>> deliveries(@PathVariable UUID id) {
        return ApiResponse.ok(notificationService.deliveries(id));
    }

    @PostMapping("/admin/notifications/account-security")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN')")
    public ApiResponse<NotificationResponse> accountSecurity(@Valid @RequestBody AccountSecurityNotificationRequest request) {
        return ApiResponse.ok(notificationService.accountSecurity(request.userId(), request.title(), request.content()), "Account security notification sent");
    }

    @PostMapping("/admin/notifications/deliveries/retry")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN')")
    public ApiResponse<List<NotificationDeliveryResponse>> retryDeliveries() {
        return ApiResponse.ok(notificationService.retryDueDeliveries(), "Notification deliveries retried");
    }
}
