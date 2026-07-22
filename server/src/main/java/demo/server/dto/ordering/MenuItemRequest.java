package demo.server.dto.ordering;

import demo.server.common.enums.MenuItemStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuItemRequest(
        @NotNull UUID branchId,
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description,
        @Size(max = 500) String imageUrl,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotNull @Min(0) Integer stockQuantity,
        @Min(0) Integer estimatedPrepMinutes,
        MenuItemStatus status
) {
}
