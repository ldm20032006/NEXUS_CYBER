package demo.server.service.auth;

import demo.server.common.enums.RoleCode;
import demo.server.dto.admin.UserAdminResponse;
import demo.server.dto.auth.CurrentUserResponse;
import demo.server.entity.auth.AppUser;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class AuthMapper {

    public CurrentUserResponse toCurrentUser(AppUser user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getFullName(),
                user.getDisplayName(),
                user.getStatus(),
                user.getBranch() == null ? null : user.getBranch().getId(),
                roles(user),
                permissions(user));
    }

    public UserAdminResponse toAdminUser(AppUser user) {
        return new UserAdminResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getFullName(),
                user.getStatus(),
                user.getBranch() == null ? null : user.getBranch().getId(),
                roles(user));
    }

    private Set<RoleCode> roles(AppUser user) {
        return user.getRoles().stream()
                .map(role -> role.getCode())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> permissions(AppUser user) {
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
