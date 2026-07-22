package demo.server.dto.auth;

import demo.server.common.enums.RoleCode;
import demo.server.common.enums.UserStatus;

import java.util.Set;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String email,
        String phone,
        String fullName,
        String displayName,
        UserStatus status,
        UUID branchId,
        Set<RoleCode> roles,
        Set<String> permissions
) {
}
