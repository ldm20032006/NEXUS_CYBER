package demo.server.dto.ordering;

import java.util.UUID;

public record MenuCategoryResponse(
        UUID id,
        UUID branchId,
        String code,
        String name,
        String description,
        Integer sortOrder,
        Boolean active
) {
}
