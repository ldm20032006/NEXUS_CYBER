package demo.server.dto.admin;

import demo.server.common.enums.RoleCode;
import demo.server.common.enums.UserStatus;

import java.util.Set;
import java.util.UUID;

public record UserAdminResponse(
        UUID id,
        String email,
        String phone,
        String fullName,
        UserStatus status,
        UUID branchId,
        Set<RoleCode> roles
) {
}
