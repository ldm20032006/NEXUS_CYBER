package demo.server.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushSubscriptionRequest(
        @NotBlank @Size(max = 2000) String endpoint,
        @NotBlank @Size(max = 500) String p256dh,
        @NotBlank @Size(max = 500) String auth,
        @Size(max = 100) String userAgent
) {
}
