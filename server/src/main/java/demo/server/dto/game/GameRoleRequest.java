package demo.server.dto.game;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GameRoleRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 100) String name,
        Integer sortOrder
) {
}
