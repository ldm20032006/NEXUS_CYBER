package demo.server.dto.admin;

import demo.server.common.enums.RoleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record CreateStaffRequest(
        @Email @NotBlank String email,
        @Size(max = 20) String phone,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 120) String fullName,
        UUID branchId,
        @NotEmpty Set<RoleCode> roles
) {
}
