package demo.server.dto.report;

import java.math.BigDecimal;

public record KpiMetricResponse(
        String code,
        String label,
        BigDecimal value,
        String unit,
        String formula
) {
}
