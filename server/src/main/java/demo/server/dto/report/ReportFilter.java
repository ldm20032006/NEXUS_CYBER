package demo.server.dto.report;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

public record ReportFilter(
        Instant from,
        Instant to,
        ZoneId timezone,
        UUID branchId,
        UUID zoneId,
        UUID stationId
) {
}
