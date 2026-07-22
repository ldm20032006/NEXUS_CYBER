package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.dto.ordering.CancelOrderRequest;
import demo.server.dto.ordering.CreateOrderRequest;
import demo.server.dto.ordering.FoodOrderResponse;
import demo.server.service.ordering.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("isAuthenticated()")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<FoodOrderResponse> create(@Valid @RequestBody CreateOrderRequest request,
                                                 @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResponse.ok(orderService.create(request, idempotencyKey), "Order created");
    }

    @GetMapping("/me")
    public ApiResponse<List<FoodOrderResponse>> myOrders() {
        return ApiResponse.ok(orderService.myOrders());
    }

    @GetMapping("/my-orders")
    public ApiResponse<List<FoodOrderResponse>> myOrdersAlias() {
        return ApiResponse.ok(orderService.myOrders());
    }

    @GetMapping("/{id}")
    public ApiResponse<FoodOrderResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(orderService.get(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<FoodOrderResponse> cancel(@PathVariable UUID id, @Valid @RequestBody CancelOrderRequest request) {
        return ApiResponse.ok(orderService.cancel(id, request), "Order cancelled");
    }
}
