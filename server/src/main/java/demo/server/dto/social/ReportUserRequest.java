package demo.server.dto.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportUserRequest(
        @NotBlank @Size(max = 1000) String reason,
        @Size(max = 1000) String context
) {
}
