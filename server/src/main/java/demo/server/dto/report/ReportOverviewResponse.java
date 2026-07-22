package demo.server.dto.report;

import java.time.Instant;
import java.util.List;

public record ReportOverviewResponse(
        Instant from,
        Instant to,
        String timezone,
        Instant generatedAt,
        List<KpiMetricResponse> kpis,
        RevenueReportResponse revenue
) {
}
