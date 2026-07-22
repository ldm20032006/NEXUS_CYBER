package demo.server.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterGamerRequest(
        @Email @NotBlank String email,
        @Size(max = 20) String phone,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 120) String fullName,
        @Size(max = 120) String displayName
) {
}
