package demo.server.dto.ordering;

import demo.server.common.enums.MenuItemStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuItemResponse(
        UUID id,
        UUID branchId,
        UUID categoryId,
        String code,
        String name,
        String description,
        String imageUrl,
        BigDecimal price,
        Integer stockQuantity,
        Integer estimatedPrepMinutes,
        MenuItemStatus status
) {
}
