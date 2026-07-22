package demo.server.dto.session;

import java.math.BigDecimal;
import java.util.UUID;

public record SessionBillingPolicyResponse(
        UUID id,
        UUID branchId,
        UUID zoneId,
        UUID stationId,
        BigDecimal hourlyRate,
        BigDecimal minimumCharge,
        Integer billingIncrementMinutes,
        boolean active
) {
}
