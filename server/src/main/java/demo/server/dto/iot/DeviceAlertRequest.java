package demo.server.dto.iot;

import demo.server.common.enums.AlertSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DeviceAlertRequest(
        @NotNull UUID deviceId,
        @NotBlank @Size(max = 100) String alertCode,
        @NotBlank @Size(max = 150) String title,
        @Size(max = 2000) String description,
        @NotNull AlertSeverity severity,
        @Size(max = 1000) String note
) {
}
