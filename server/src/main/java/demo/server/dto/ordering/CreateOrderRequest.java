package demo.server.dto.ordering;

import demo.server.common.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        PaymentMethod paymentMethod,
        @Size(max = 1000) String note,
        @NotEmpty List<@Valid CreateOrderItemRequest> items
) {
}
