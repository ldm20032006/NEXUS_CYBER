package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.dto.admin.CreateStaffRequest;
import demo.server.dto.admin.LockUserRequest;
import demo.server.dto.admin.PermissionResponse;
import demo.server.dto.admin.RoleResponse;
import demo.server.dto.admin.UserAdminResponse;
import demo.server.service.auth.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN')")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @PostMapping("/staff")
    public ApiResponse<UserAdminResponse> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return ApiResponse.ok(userAdminService.createStaff(request), "Staff user created");
    }

    @GetMapping
    public ApiResponse<List<UserAdminResponse>> listUsers() {
        return ApiResponse.ok(userAdminService.listUsers());
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleResponse>> listRoles() {
        return ApiResponse.ok(userAdminService.listRoles());
    }

    @GetMapping("/permissions")
    public ApiResponse<List<PermissionResponse>> listPermissions() {
        return ApiResponse.ok(userAdminService.listPermissions());
    }

    @PatchMapping("/{id}/lock")
    public ApiResponse<UserAdminResponse> lockUser(@PathVariable UUID id, @Valid @RequestBody LockUserRequest request) {
        return ApiResponse.ok(userAdminService.lockUser(id, request.reason()), "User locked");
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<UserAdminResponse> activateUser(@PathVariable UUID id) {
        return ApiResponse.ok(userAdminService.activateUser(id), "User activated");
    }
}
