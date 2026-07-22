package demo.server.dto.ordering;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateOrderItemRequest(
        @NotNull UUID menuItemId,
        @NotNull @Min(1) Integer quantity,
        @Size(max = 1000) String note
) {
}
