package demo.server.dto.session;

import jakarta.validation.constraints.Size;

public record EndSessionRequest(
        @Size(max = 500) String reason
) {
}
