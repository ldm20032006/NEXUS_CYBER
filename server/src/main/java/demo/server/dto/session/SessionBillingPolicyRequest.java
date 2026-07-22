package demo.server.dto.session;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record SessionBillingPolicyRequest(
        @NotNull
        UUID branchId,

        UUID zoneId,

        UUID stationId,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal hourlyRate,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal minimumCharge,

        @NotNull
        @Min(1)
        Integer billingIncrementMinutes,

        Boolean active
) {
}
