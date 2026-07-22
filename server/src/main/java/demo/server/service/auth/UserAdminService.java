package demo.server.service.auth;

import demo.server.common.enums.RoleCode;
import demo.server.common.enums.UserStatus;
import demo.server.common.security.AuthenticatedUser;
import demo.server.dto.admin.CreateStaffRequest;
import demo.server.dto.admin.PermissionResponse;
import demo.server.dto.admin.RoleResponse;
import demo.server.dto.admin.UserAdminResponse;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.Permission;
import demo.server.entity.auth.Role;
import demo.server.entity.branch.Branch;
import demo.server.exception.BusinessRuleException;
import demo.server.exception.DuplicateResourceException;
import demo.server.exception.ForbiddenException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.auth.PermissionRepository;
import demo.server.repository.auth.RoleRepository;
import demo.server.repository.branch.BranchRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserAdminService {

    private static final Set<RoleCode> STAFF_CREATABLE_ROLES = Set.of(
            RoleCode.STAFF_FNB,
            RoleCode.STAFF_TECHNICAL,
            RoleCode.BRANCH_ADMIN,
            RoleCode.STATION_CLIENT
    );

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;

    public UserAdminService(
            AppUserRepository appUserRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            BranchRepository branchRepository,
            PasswordEncoder passwordEncoder,
            AuthMapper authMapper
    ) {
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
        this.authMapper = authMapper;
    }

    @Transactional
    public UserAdminResponse createStaff(CreateStaffRequest request) {
        AuthenticatedUser actor = actor();
        Set<RoleCode> requestedRoles = new LinkedHashSet<>(request.roles());
        if (requestedRoles.contains(RoleCode.GAMER)) {
            throw new BusinessRuleException("Use gamer registration for GAMER accounts");
        }
        if (!STAFF_CREATABLE_ROLES.containsAll(requestedRoles) && !actor.roles().contains(RoleCode.SUPER_ADMIN)) {
            throw new ForbiddenException("Requested role is not allowed");
        }
        if (actor.roles().contains(RoleCode.BRANCH_ADMIN)) {
            enforceBranchAdminCreateRules(actor, request, requestedRoles);
        }
        ensureUnique(request.email(), request.phone());
        List<Role> roles = roleRepository.findByCodeIn(requestedRoles);
        if (roles.size() != requestedRoles.size()) {
            throw new ResourceNotFoundException("One or more roles are not configured");
        }
        Branch branch = resolveBranch(request.branchId(), actor);
        AppUser user = new AppUser();
        user.setEmail(normalize(request.email()));
        user.setPhone(blankToNull(request.phone()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setStatus(UserStatus.ACTIVE);
        user.setActivatedAt(Instant.now());
        user.setBranch(branch);
        user.getRoles().addAll(roles);
        return authMapper.toAdminUser(appUserRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserAdminResponse> listUsers() {
        AuthenticatedUser actor = actor();
        List<AppUser> users;
        if (actor.roles().contains(RoleCode.SUPER_ADMIN)) {
            users = appUserRepository.findAll();
        } else {
            if (actor.branchId() == null) {
                throw new ForbiddenException("Branch scoped user has no branch");
            }
            users = appUserRepository.findAllByBranch_Id(actor.branchId());
        }
        return users.stream().map(authMapper::toAdminUser).toList();
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream().map(this::toRoleResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll().stream().map(this::toPermissionResponse).toList();
    }

    @Transactional
    public UserAdminResponse lockUser(UUID userId, String reason) {
        AppUser user = scopedTarget(userId);
        user.setStatus(UserStatus.LOCKED);
        user.setLockedAt(Instant.now());
        user.setLockReason(reason);
        return authMapper.toAdminUser(user);
    }

    @Transactional
    public UserAdminResponse activateUser(UUID userId) {
        AppUser user = scopedTarget(userId);
        user.setStatus(UserStatus.ACTIVE);
        user.setActivatedAt(Instant.now());
        user.setLockedAt(null);
        user.setLockReason(null);
        return authMapper.toAdminUser(user);
    }

    private void enforceBranchAdminCreateRules(AuthenticatedUser actor, CreateStaffRequest request, Set<RoleCode> requestedRoles) {
        if (requestedRoles.contains(RoleCode.SUPER_ADMIN)) {
            throw new ForbiddenException("Branch Admin cannot create Super Admin");
        }
        if (requestedRoles.contains(RoleCode.BRANCH_ADMIN)) {
            throw new ForbiddenException("Branch Admin cannot create another Branch Admin");
        }
        if (actor.branchId() == null || !actor.branchId().equals(request.branchId())) {
            throw new ForbiddenException("Branch Admin can only create staff in own branch");
        }
    }

    private AppUser scopedTarget(UUID userId) {
        AuthenticatedUser actor = actor();
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!actor.roles().contains(RoleCode.SUPER_ADMIN)) {
            UUID targetBranchId = user.getBranch() == null ? null : user.getBranch().getId();
            if (actor.branchId() == null || !actor.branchId().equals(targetBranchId)) {
                throw new ForbiddenException("User is outside branch scope");
            }
            Set<RoleCode> targetRoles = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
            if (targetRoles.contains(RoleCode.SUPER_ADMIN) || targetRoles.contains(RoleCode.BRANCH_ADMIN)) {
                throw new ForbiddenException("Branch Admin cannot administer privileged accounts");
            }
        }
        return user;
    }

    private Branch resolveBranch(UUID branchId, AuthenticatedUser actor) {
        UUID effectiveBranchId = branchId;
        if (!actor.roles().contains(RoleCode.SUPER_ADMIN)) {
            effectiveBranchId = actor.branchId();
        }
        if (effectiveBranchId == null) {
            return null;
        }
        return branchRepository.findById(effectiveBranchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
    }

    private AuthenticatedUser actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ForbiddenException("Authenticated administrator is required");
        }
        return user;
    }

    private void ensureUnique(String email, String phone) {
        if (appUserRepository.existsByEmail(normalize(email))) {
            throw new DuplicateResourceException("Email already exists");
        }
        if (StringUtils.hasText(phone) && appUserRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("Phone already exists");
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private RoleResponse toRoleResponse(Role role) {
        Set<PermissionResponse> permissions = role.getPermissions().stream()
                .map(this::toPermissionResponse)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new RoleResponse(role.getId(), role.getCode(), role.getName(), role.getDescription(), permissions);
    }

    private PermissionResponse toPermissionResponse(Permission permission) {
        return new PermissionResponse(permission.getId(), permission.getCode(), permission.getName(), permission.getDescription());
    }
}
