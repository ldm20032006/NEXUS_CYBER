package demo.server.dto.iot;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AssignDeviceAlertRequest(
        @NotNull UUID staffId,
        @Size(max = 1000) String note
) {
}
