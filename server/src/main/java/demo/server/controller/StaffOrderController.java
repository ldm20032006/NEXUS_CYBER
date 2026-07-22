package demo.server.controller;

import demo.server.common.enums.OrderStatus;
import demo.server.common.response.ApiResponse;
import demo.server.dto.ordering.CancelOrderRequest;
import demo.server.dto.ordering.FoodOrderResponse;
import demo.server.dto.ordering.UpdateOrderStatusRequest;
import demo.server.service.ordering.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/staff/orders")
@PreAuthorize("hasAnyRole('STAFF_FNB','BRANCH_ADMIN','SUPER_ADMIN')")
public class StaffOrderController {

    private final OrderService orderService;

    public StaffOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<List<FoodOrderResponse>> queue(@RequestParam(required = false) OrderStatus status) {
        return ApiResponse.ok(orderService.staffOrders(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<FoodOrderResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(orderService.get(id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<FoodOrderResponse> status(@PathVariable UUID id,
                                                 @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ApiResponse.ok(orderService.updateStatus(id, request.status()), "Order status updated");
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<FoodOrderResponse> cancel(@PathVariable UUID id, @Valid @RequestBody CancelOrderRequest request) {
        return ApiResponse.ok(orderService.cancel(id, request), "Order cancelled");
    }
}
