package demo.server.dto.admin;

import demo.server.common.enums.RoleCode;

import java.util.Set;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        RoleCode code,
        String name,
        String description,
        Set<PermissionResponse> permissions
) {
}
