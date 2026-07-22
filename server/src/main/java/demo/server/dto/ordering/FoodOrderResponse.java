package demo.server.dto.ordering;

import demo.server.common.enums.OrderStatus;
import demo.server.common.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FoodOrderResponse(
        UUID id,
        UUID userId,
        UUID branchId,
        UUID stationId,
        UUID playSessionId,
        OrderStatus status,
        PaymentMethod paymentMethod,
        BigDecimal totalAmount,
        String note,
        String cancelReason,
        Instant createdAt,
        Instant acceptedAt,
        Instant preparingAt,
        Instant readyAt,
        Instant deliveredAt,
        Instant cancelledAt,
        List<OrderItemResponse> items
) {
}
