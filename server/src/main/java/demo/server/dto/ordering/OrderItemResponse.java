package demo.server.dto.ordering;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID menuItemId,
        String itemName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        String note
) {
}
