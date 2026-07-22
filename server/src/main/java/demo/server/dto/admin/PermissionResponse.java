package demo.server.dto.admin;

import java.util.UUID;

public record PermissionResponse(
        UUID id,
        String code,
        String name,
        String description
) {
}
