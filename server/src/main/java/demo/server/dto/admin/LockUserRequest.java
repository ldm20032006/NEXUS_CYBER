package demo.server.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LockUserRequest(@NotBlank @Size(max = 500) String reason) {
}
