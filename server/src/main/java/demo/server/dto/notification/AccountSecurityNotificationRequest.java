package demo.server.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AccountSecurityNotificationRequest(
        @NotNull UUID userId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 2000) String content
) {
}
