package demo.server.dto.social;

import jakarta.validation.constraints.Size;

public record BlockUserRequest(
        @Size(max = 500) String reason
) {
}
