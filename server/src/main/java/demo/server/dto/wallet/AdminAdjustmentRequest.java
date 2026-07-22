package demo.server.dto.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdminAdjustmentRequest(
        @NotNull
        @DecimalMin(value = "-999999999999.99", inclusive = true)
        BigDecimal amount,

        @NotBlank
        @Size(max = 500)
        String reason
) {
}
