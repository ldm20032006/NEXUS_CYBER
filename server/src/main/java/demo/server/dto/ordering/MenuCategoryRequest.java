package demo.server.dto.ordering;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MenuCategoryRequest(
        @NotNull UUID branchId,
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String description,
        Integer sortOrder,
        Boolean active
) {
}
