package demo.server.service.report;

import demo.server.common.audit.AuditRecorder;
import demo.server.common.enums.AuditAction;
import demo.server.common.time.ClockProvider;
import demo.server.dto.report.KpiMetricResponse;
import demo.server.dto.report.ReportFilter;
import demo.server.dto.report.ReportOverviewResponse;
import demo.server.dto.report.ReportTableRow;
import demo.server.dto.report.RevenueReportResponse;
import demo.server.exception.BusinessRuleException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.repository.branch.StationRepository;
import demo.server.service.branch.BranchScope;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReportService {

    private static final long MAX_RANGE_DAYS = 366;

    private final EntityManager entityManager;
    private final BranchScope branchScope;
    private final StationRepository stationRepository;
    private final ClockProvider clockProvider;
    private final AuditRecorder auditRecorder;

    public ReportService(EntityManager entityManager, BranchScope branchScope, StationRepository stationRepository,
                         ClockProvider clockProvider, AuditRecorder auditRecorder) {
        this.entityManager = entityManager;
        this.branchScope = branchScope;
        this.stationRepository = stationRepository;
        this.clockProvider = clockProvider;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "reportOverview", key = "{#period,#from,#to,#timezone,#branchId,#zoneId,#stationId}")
    public ReportOverviewResponse overview(String period, Instant from, Instant to, String timezone,
                                           UUID branchId, UUID zoneId, UUID stationId) {
        ReportFilter filter = filter(period, from, to, timezone, branchId, zoneId, stationId);
        RevenueReportResponse revenue = revenue(filter);
        List<KpiMetricResponse> kpis = new ArrayList<>();
        long totalStations = countStations(filter);
        long occupiedStations = countOccupiedStations(filter);
        BigDecimal occupancy = totalStations == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf(occupiedStations).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalStations), 2, RoundingMode.HALF_UP);
        kpis.add(metric("stations.total", "Stations", totalStations, "count", "count(stations) matching branch/zone/station filter"));
        kpis.add(metric("occupancy.rate", "Occupancy Rate", occupancy, "percent", "occupied stations / total active stations * 100"));
        kpis.add(metric("sessions.total", "Sessions", countSessions(filter), "count", "count(play_sessions.started_at in range)"));
        kpis.add(metric("gamers.active", "Active Gamers", countActiveGamers(filter), "count", "count(distinct play_sessions.user_id in range)"));
        kpis.add(metric("orders.total", "Orders", countOrders(filter), "count", "count(food_orders.created_at in range excluding deleted)"));
        kpis.add(metric("alerts.open", "Open Alerts", countOpenAlerts(filter), "count", "count(device_alerts status OPEN/ACKNOWLEDGED/IN_PROGRESS/REOPENED)"));
        kpis.add(metric("device.failures", "Device Failures", countDeviceFailures(filter), "count", "count(device_alerts severity CRITICAL/HIGH or code HEARTBEAT_MISSED in range)"));
        kpis.add(metric("lfg.success.rate", "LFG Success Rate", lfgSuccessRate(filter), "percent", "accepted team invitations / all final team invitations * 100"));
        kpis.add(metric("revenue.net", "Net Revenue", revenue.netRevenue(), "VND", revenue.formula()));
        ReportOverviewResponse response = new ReportOverviewResponse(filter.from(), filter.to(), filter.timezone().toString(), clockProvider.now(), kpis, revenue);
        auditRecorder.record(AuditAction.VIEW_REPORT, "Report", null, null, Map.of("type", "overview", "from", filter.from(), "to", filter.to()));
        return response;
    }

    @Transactional(readOnly = true)
    public RevenueReportResponse revenue(String period, Instant from, Instant to, String timezone,
                                         UUID branchId, UUID zoneId, UUID stationId) {
        return revenue(filter(period, from, to, timezone, branchId, zoneId, stationId));
    }

    @Transactional(readOnly = true)
    public List<ReportTableRow> rows(String reportType, String period, Instant from, Instant to, String timezone,
                                     UUID branchId, UUID zoneId, UUID stationId) {
        ReportOverviewResponse overview = overview(period, from, to, timezone, branchId, zoneId, stationId);
        if ("revenue".equalsIgnoreCase(reportType)) {
            RevenueReportResponse revenue = overview.revenue();
            return List.of(
                    new ReportTableRow("sessionRevenue", revenue.sessionRevenue(), "VND", "sum SESSION_CHARGE"),
                    new ReportTableRow("foodRevenue", revenue.foodRevenue(), "VND", "sum ORDER_PAYMENT"),
                    new ReportTableRow("topUpRevenue", revenue.topUpRevenue(), "VND", "sum TOP_UP"),
                    new ReportTableRow("refundAmount", revenue.refundAmount(), "VND", "sum REFUND"),
                    new ReportTableRow("netRevenue", revenue.netRevenue(), "VND", revenue.formula()));
        }
        return overview.kpis().stream().map(kpi -> new ReportTableRow(kpi.code(), kpi.value(), kpi.unit(), kpi.formula())).toList();
    }

    @Transactional(readOnly = true)
    public byte[] csv(String reportType, String period, Instant from, Instant to, String timezone,
                      UUID branchId, UUID zoneId, UUID stationId) {
        ReportFilter filter = filter(period, from, to, timezone, branchId, zoneId, stationId);
        List<ReportTableRow> rows = rows(reportType, period, from, to, timezone, branchId, zoneId, stationId);
        StringBuilder csv = new StringBuilder();
        csv.append("from,to,timezone,generatedAt,metric,value,unit,formula\n");
        Instant generatedAt = clockProvider.now();
        for (ReportTableRow row : rows) {
            csv.append(escape(filter.from().toString())).append(',')
                    .append(escape(filter.to().toString())).append(',')
                    .append(escape(filter.timezone().toString())).append(',')
                    .append(escape(generatedAt.toString())).append(',')
                    .append(escape(row.metric())).append(',')
                    .append(row.value()).append(',')
                    .append(escape(row.unit())).append(',')
                    .append(escape(row.formula())).append('\n');
        }
        auditRecorder.record(AuditAction.EXPORT_REPORT, "Report", null, null,
                Map.of("type", reportType, "format", "csv", "from", filter.from(), "to", filter.to()));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private RevenueReportResponse revenue(ReportFilter filter) {
        BigDecimal session = walletSum(filter, "SESSION_CHARGE").abs();
        BigDecimal food = walletSum(filter, "ORDER_PAYMENT").abs();
        BigDecimal topUp = walletSum(filter, "TOP_UP");
        BigDecimal refund = walletSum(filter, "REFUND");
        BigDecimal net = session.add(food).add(topUp).subtract(refund).setScale(2, RoundingMode.HALF_UP);
        return new RevenueReportResponse(session, food, topUp, refund, net,
                "netRevenue = SESSION_CHARGE + ORDER_PAYMENT + TOP_UP - REFUND; ADMIN_ADJUSTMENT excluded");
    }

    private BigDecimal walletSum(ReportFilter filter, String type) {
        String sql = """
                select coalesce(sum(wt.amount), 0)
                from wallet_transactions wt
                join users u on u.id = wt.user_id
                left join food_orders fo on wt.reference_type = 'FOOD_ORDER' and wt.reference_id = cast(fo.id as varchar)
                left join play_sessions ps on wt.reference_type = 'PLAY_SESSION' and wt.reference_id = cast(ps.id as varchar)
                left join stations rs on rs.id = coalesce(fo.station_id, ps.station_id)
                where wt.type = :type and wt.created_at >= :from and wt.created_at < :to
                  and (:branchId is null or coalesce(fo.branch_id, rs.branch_id, u.branch_id) = :branchId)
                  and (:zoneId is null or rs.zone_id = :zoneId)
                  and (:stationId is null or rs.id = :stationId)
                """;
        return decimal(query(sql, filter).setParameter("type", type).getSingleResult());
    }

    private long countStations(ReportFilter filter) {
        String sql = """
                select count(*) from stations s
                where s.deleted = false
                  and (:branchId is null or s.branch_id = :branchId)
                  and (:zoneId is null or s.zone_id = :zoneId)
                  and (:stationId is null or s.id = :stationId)
                """;
        return number(query(sql, filter).getSingleResult());
    }

    private long countOccupiedStations(ReportFilter filter) {
        String sql = """
                select count(*) from stations s
                where s.deleted = false and s.status = 'OCCUPIED'
                  and (:branchId is null or s.branch_id = :branchId)
                  and (:zoneId is null or s.zone_id = :zoneId)
                  and (:stationId is null or s.id = :stationId)
                """;
        return number(query(sql, filter).getSingleResult());
    }

    private long countSessions(ReportFilter filter) {
        String sql = """
                select count(*) from play_sessions ps
                join stations s on s.id = ps.station_id
                where ps.started_at >= :from and ps.started_at < :to
                  and (:branchId is null or s.branch_id = :branchId)
                  and (:zoneId is null or s.zone_id = :zoneId)
                  and (:stationId is null or s.id = :stationId)
                """;
        return number(query(sql, filter).getSingleResult());
    }

    private long countActiveGamers(ReportFilter filter) {
        String sql = """
                select count(distinct ps.user_id) from play_sessions ps
                join stations s on s.id = ps.station_id
                where ps.started_at >= :from and ps.started_at < :to
                  and (:branchId is null or s.branch_id = :branchId)
                  and (:zoneId is null or s.zone_id = :zoneId)
                  and (:stationId is null or s.id = :stationId)
                """;
        return number(query(sql, filter).getSingleResult());
    }

    private long countOrders(ReportFilter filter) {
        String sql = """
                select count(*) from food_orders fo
                left join stations s on s.id = fo.station_id
                where fo.deleted = false and fo.created_at >= :from and fo.created_at < :to
                  and (:branchId is null or fo.branch_id = :branchId)
                  and (:zoneId is null or s.zone_id = :zoneId)
                  and (:stationId is null or s.id = :stationId)
                """;
        return number(query(sql, filter).getSingleResult());
    }

    private long countOpenAlerts(ReportFilter filter) {
        String sql = """
                select count(*) from device_alerts da
                where da.deleted = false and da.status in ('OPEN','ACKNOWLEDGED','IN_PROGRESS','REOPENED')
                  and (:branchId is null or da.branch_id = :branchId)
                  and (:stationId is null or da.station_id = :stationId)
                """;
        return number(query(sql, filter).getSingleResult());
    }

    private long countDeviceFailures(ReportFilter filter) {
        String sql = """
                select count(*) from device_alerts da
                where da.deleted = false and da.created_at >= :from and da.created_at < :to
                  and (da.severity in ('CRITICAL','HIGH') or da.alert_code = 'HEARTBEAT_MISSED')
                  and (:branchId is null or da.branch_id = :branchId)
                  and (:stationId is null or da.station_id = :stationId)
                """;
        return number(query(sql, filter).getSingleResult());
    }

    private BigDecimal lfgSuccessRate(ReportFilter filter) {
        String sql = """
                select
                    coalesce(sum(case when ti.status = 'ACCEPTED' then 1 else 0 end), 0),
                    count(*)
                from team_invitations ti
                left join lobbies l on l.id = ti.lobby_id
                where ti.created_at >= :from and ti.created_at < :to
                  and ti.status in ('ACCEPTED','REJECTED','CANCELLED','EXPIRED')
                  and (:branchId is null or l.branch_id = :branchId)
                  and (:zoneId is null or l.zone_id = :zoneId)
                """;
        Object[] row = (Object[]) query(sql, filter).getSingleResult();
        BigDecimal accepted = decimal(row[0]);
        BigDecimal total = decimal(row[1]);
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return accepted.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private Query query(String sql, ReportFilter filter) {
        Query query = entityManager.createNativeQuery(sql);
        if (sql.contains(":from")) {
            query.setParameter("from", filter.from());
        }
        if (sql.contains(":to")) {
            query.setParameter("to", filter.to());
        }
        if (sql.contains(":branchId")) {
            query.setParameter("branchId", filter.branchId());
        }
        if (sql.contains(":zoneId")) {
            query.setParameter("zoneId", filter.zoneId());
        }
        if (sql.contains(":stationId")) {
            query.setParameter("stationId", filter.stationId());
        }
        return query;
    }

    private ReportFilter filter(String period, Instant from, Instant to, String timezone, UUID branchId, UUID zoneId, UUID stationId) {
        ZoneId zone = timezone == null || timezone.isBlank() ? ZoneId.of("UTC") : ZoneId.of(timezone);
        Instant[] range = resolveRange(period, from, to, zone);
        if (!range[0].isBefore(range[1])) {
            throw new BusinessRuleException("Report from must be before to");
        }
        if (Duration.between(range[0], range[1]).toDays() > MAX_RANGE_DAYS) {
            throw new BusinessRuleException("Report date range cannot exceed " + MAX_RANGE_DAYS + " days");
        }
        UUID scopedBranch = branchScope.requireScopedBranch(branchId);
        if (stationId != null) {
            var station = stationRepository.findById(stationId).orElseThrow(() -> new ResourceNotFoundException("Station not found"));
            branchScope.assertBranchAllowed(station.getBranch().getId());
            if (scopedBranch != null && !station.getBranch().getId().equals(scopedBranch)) {
                throw new BusinessRuleException("Station is outside selected branch");
            }
            scopedBranch = station.getBranch().getId();
        }
        return new ReportFilter(range[0], range[1], zone, scopedBranch, zoneId, stationId);
    }

    private Instant[] resolveRange(String period, Instant from, Instant to, ZoneId zone) {
        Instant now = clockProvider.now();
        if (from != null && to != null) {
            return new Instant[]{from, to};
        }
        ZonedDateTime zonedNow = now.atZone(zone);
        String normalized = period == null ? "date" : period.toLowerCase();
        return switch (normalized) {
            case "week" -> {
                ZonedDateTime start = zonedNow.toLocalDate().minusDays(zonedNow.getDayOfWeek().getValue() - 1L).atStartOfDay(zone);
                yield new Instant[]{start.toInstant(), start.plusWeeks(1).toInstant()};
            }
            case "month" -> {
                YearMonth ym = YearMonth.from(zonedNow);
                ZonedDateTime start = ym.atDay(1).atStartOfDay(zone);
                yield new Instant[]{start.toInstant(), start.plusMonths(1).toInstant()};
            }
            default -> {
                LocalDate day = zonedNow.toLocalDate();
                ZonedDateTime start = day.atStartOfDay(zone);
                yield new Instant[]{start.toInstant(), start.plusDays(1).toInstant()};
            }
        };
    }

    private KpiMetricResponse metric(String code, String label, long value, String unit, String formula) {
        return metric(code, label, BigDecimal.valueOf(value), unit, formula);
    }

    private KpiMetricResponse metric(String code, String label, BigDecimal value, String unit, String formula) {
        return new KpiMetricResponse(code, label, value.setScale(value.scale() < 0 ? 0 : value.scale(), RoundingMode.HALF_UP), unit, formula);
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private String escape(String value) {
        String text = value == null ? "" : value;
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
