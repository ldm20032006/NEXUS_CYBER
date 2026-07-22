package demo.server.dto.report;

import java.math.BigDecimal;

public record ReportTableRow(
        String metric,
        BigDecimal value,
        String unit,
        String formula
) {
}
