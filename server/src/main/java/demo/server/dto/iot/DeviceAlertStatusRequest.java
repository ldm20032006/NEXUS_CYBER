package demo.server.dto.iot;

import demo.server.common.enums.AlertStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeviceAlertStatusRequest(
        @NotNull AlertStatus status,
        @Size(max = 1000) String note
) {
}
