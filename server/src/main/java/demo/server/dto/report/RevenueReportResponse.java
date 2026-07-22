package demo.server.dto.report;

import java.math.BigDecimal;

public record RevenueReportResponse(
        BigDecimal sessionRevenue,
        BigDecimal foodRevenue,
        BigDecimal topUpRevenue,
        BigDecimal refundAmount,
        BigDecimal netRevenue,
        String formula
) {
}
