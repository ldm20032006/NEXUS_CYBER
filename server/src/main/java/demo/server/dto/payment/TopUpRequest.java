package demo.server.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TopUpRequest(
        @NotNull
        @DecimalMin("1.00")
        BigDecimal amount
) {
}
