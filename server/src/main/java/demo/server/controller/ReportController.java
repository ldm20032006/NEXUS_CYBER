package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.dto.report.ReportOverviewResponse;
import demo.server.dto.report.ReportTableRow;
import demo.server.dto.report.RevenueReportResponse;
import demo.server.service.report.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping({"/dashboard/overview", "/reports/overview"})
    public ApiResponse<ReportOverviewResponse> overview(@RequestParam(defaultValue = "date") String period,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                                        @RequestParam(defaultValue = "UTC") String timezone,
                                                        @RequestParam(required = false) UUID branchId,
                                                        @RequestParam(required = false) UUID zoneId,
                                                        @RequestParam(required = false) UUID stationId) {
        return ApiResponse.ok(reportService.overview(period, from, to, timezone, branchId, zoneId, stationId));
    }

    @GetMapping("/reports/revenue")
    public ApiResponse<RevenueReportResponse> revenue(@RequestParam(defaultValue = "date") String period,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                                      @RequestParam(defaultValue = "UTC") String timezone,
                                                      @RequestParam(required = false) UUID branchId,
                                                      @RequestParam(required = false) UUID zoneId,
                                                      @RequestParam(required = false) UUID stationId) {
        return ApiResponse.ok(reportService.revenue(period, from, to, timezone, branchId, zoneId, stationId));
    }

    @GetMapping({"/reports/sessions", "/reports/orders", "/reports/devices"})
    public ApiResponse<List<ReportTableRow>> reportRows(@RequestParam(defaultValue = "overview") String type,
                                                        @RequestParam(defaultValue = "date") String period,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                                        @RequestParam(defaultValue = "UTC") String timezone,
                                                        @RequestParam(required = false) UUID branchId,
                                                        @RequestParam(required = false) UUID zoneId,
                                                        @RequestParam(required = false) UUID stationId) {
        return ApiResponse.ok(reportService.rows(type, period, from, to, timezone, branchId, zoneId, stationId));
    }

    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "overview") String type,
                                         @RequestParam(defaultValue = "csv") String format,
                                         @RequestParam(defaultValue = "date") String period,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                         @RequestParam(defaultValue = "UTC") String timezone,
                                         @RequestParam(required = false) UUID branchId,
                                         @RequestParam(required = false) UUID zoneId,
                                         @RequestParam(required = false) UUID stationId) {
        if (!"csv".equalsIgnoreCase(format)) {
            throw new demo.server.exception.BusinessRuleException("Only CSV export is currently supported");
        }
        byte[] body = reportService.csv(type, period, from, to, timezone, branchId, zoneId, stationId);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("nexus-" + type + "-report.csv")
                        .build()
                        .toString())
                .body(body);
    }
}
