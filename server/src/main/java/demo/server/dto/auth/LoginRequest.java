package demo.server.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 150) String identifier,
        @NotBlank @Size(max = 100) String password
) {
}
