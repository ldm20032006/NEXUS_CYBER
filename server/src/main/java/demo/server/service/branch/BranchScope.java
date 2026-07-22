package demo.server.service.branch;

import demo.server.common.enums.RoleCode;
import demo.server.common.security.AuthenticatedUser;
import demo.server.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BranchScope {

    public AuthenticatedUser actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ForbiddenException("Authenticated administrator is required");
        }
        return user;
    }

    public boolean isSuperAdmin(AuthenticatedUser actor) {
        return actor.roles().contains(RoleCode.SUPER_ADMIN);
    }

    public UUID requireScopedBranch(UUID requestedBranchId) {
        AuthenticatedUser actor = actor();
        if (isSuperAdmin(actor)) {
            return requestedBranchId;
        }
        if (actor.branchId() == null) {
            throw new ForbiddenException("Branch scoped user has no branch");
        }
        if (requestedBranchId != null && !actor.branchId().equals(requestedBranchId)) {
            throw new ForbiddenException("Resource is outside branch scope");
        }
        return actor.branchId();
    }

    public void assertBranchAllowed(UUID branchId) {
        requireScopedBranch(branchId);
    }

    public void requireSuperAdmin() {
        if (!isSuperAdmin(actor())) {
            throw new ForbiddenException("Super Admin role is required");
        }
    }
}
