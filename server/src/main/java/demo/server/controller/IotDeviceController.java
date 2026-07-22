package demo.server.controller;

import demo.server.common.enums.AlertStatus;
import demo.server.common.response.ApiResponse;
import demo.server.common.response.PageResponse;
import demo.server.dto.iot.AlertHistoryResponse;
import demo.server.dto.iot.AssignDeviceAlertRequest;
import demo.server.dto.iot.DeviceAlertRequest;
import demo.server.dto.iot.DeviceAlertResponse;
import demo.server.dto.iot.DeviceAlertStatusRequest;
import demo.server.dto.iot.DeviceHeartbeatRequest;
import demo.server.dto.iot.DeviceTelemetryRequest;
import demo.server.dto.iot.DeviceTelemetryResponse;
import demo.server.dto.iot.IotDeviceRequest;
import demo.server.dto.iot.IotDeviceResponse;
import demo.server.service.iot.DeviceAlertService;
import demo.server.service.iot.IotDeviceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
public class IotDeviceController {

    private final IotDeviceService deviceService;
    private final DeviceAlertService alertService;

    public IotDeviceController(IotDeviceService deviceService, DeviceAlertService alertService) {
        this.deviceService = deviceService;
        this.alertService = alertService;
    }

    @PostMapping("/api/v1/admin/devices")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN')")
    public ApiResponse<IotDeviceResponse> createDevice(@Valid @RequestBody IotDeviceRequest request) {
        return ApiResponse.ok(deviceService.create(request), "Device created");
    }

    @GetMapping("/api/v1/admin/devices")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN','STAFF_TECHNICAL')")
    public ApiResponse<PageResponse<IotDeviceResponse>> listDevices(@RequestParam(required = false) UUID branchId,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.from(deviceService.list(branchId, page, size)));
    }

    @GetMapping("/api/v1/admin/devices/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN','STAFF_TECHNICAL')")
    public ApiResponse<IotDeviceResponse> getDevice(@PathVariable UUID id) {
        return ApiResponse.ok(deviceService.get(id));
    }

    @PutMapping("/api/v1/admin/devices/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN')")
    public ApiResponse<IotDeviceResponse> updateDevice(@PathVariable UUID id, @Valid @RequestBody IotDeviceRequest request) {
        return ApiResponse.ok(deviceService.update(id, request), "Device updated");
    }

    @DeleteMapping("/api/v1/admin/devices/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN')")
    public ApiResponse<Void> deleteDevice(@PathVariable UUID id) {
        deviceService.delete(id);
        return ApiResponse.ok(null, "Device deleted");
    }

    @PostMapping("/api/v1/iot/devices/{id}/heartbeat")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN','STAFF_TECHNICAL','STATION_CLIENT')")
    public ApiResponse<IotDeviceResponse> heartbeat(@PathVariable UUID id, @Valid @RequestBody DeviceHeartbeatRequest request) {
        return ApiResponse.ok(deviceService.heartbeat(id, request), "Device heartbeat accepted");
    }

    @PostMapping("/api/v1/admin/devices/{id}/missed-heartbeat")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN','STAFF_TECHNICAL')")
    public ApiResponse<IotDeviceResponse> missedHeartbeat(@PathVariable UUID id) {
        return ApiResponse.ok(deviceService.missedHeartbeat(id), "Device missed heartbeat recorded");
    }

    @PostMapping("/api/v1/iot/devices/{id}/telemetry")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN','STAFF_TECHNICAL','STATION_CLIENT')")
    public ApiResponse<DeviceTelemetryResponse> telemetry(@PathVariable UUID id,
                                                          @Valid @RequestBody DeviceTelemetryRequest request) {
        return ApiResponse.ok(deviceService.recordTelemetry(id, request), "Telemetry accepted");
    }

    @GetMapping("/api/v1/iot/devices/{id}/telemetry")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN','STAFF_TECHNICAL')")
    public ApiResponse<PageResponse<DeviceTelemetryResponse>> telemetry(@PathVariable UUID id,
                                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size,
                                                                        @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(PageResponse.from(deviceService.telemetry(id, from, to, page, size, limit)));
    }

    @PostMapping("/api/v1/staff/device-alerts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN','STAFF_TECHNICAL')")
    public ApiResponse<DeviceAlertResponse> createAlert(@Valid @RequestBody DeviceAlertRequest request) {
        return ApiResponse.ok(alertService.create(request), "Device alert created");
    }

    @GetMapping({"/api/v1/staff/device-alerts", "/api/v1/admin/device-alerts"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN','STAFF_TECHNICAL')")
    public ApiResponse<PageResponse<DeviceAlertResponse>> listAlerts(@RequestParam(required = false) UUID branchId,
                                                                     @RequestParam(required = false) AlertStatus status,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.from(alertService.list(branchId, status, page, size)));
    }

    @GetMapping({"/api/v1/staff/device-alerts/{id}", "/api/v1/admin/device-alerts/{id}"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN','STAFF_TECHNICAL')")
    public ApiResponse<DeviceAlertResponse> getAlert(@PathVariable UUID id) {
        return ApiResponse.ok(alertService.get(id));
    }

    @PatchMapping({"/api/v1/staff/device-alerts/{id}", "/api/v1/admin/device-alerts/{id}"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN','STAFF_TECHNICAL')")
    public ApiResponse<DeviceAlertResponse> updateAlertStatus(@PathVariable UUID id,
                                                              @Valid @RequestBody DeviceAlertStatusRequest request) {
        return ApiResponse.ok(alertService.transition(id, request), "Device alert updated");
    }

    @PatchMapping({"/api/v1/staff/device-alerts/{id}/assign", "/api/v1/admin/device-alerts/{id}/assign"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN','STAFF_TECHNICAL')")
    public ApiResponse<DeviceAlertResponse> assignAlert(@PathVariable UUID id,
                                                        @Valid @RequestBody AssignDeviceAlertRequest request) {
        return ApiResponse.ok(alertService.assign(id, request), "Device alert assigned");
    }

    @GetMapping({"/api/v1/staff/device-alerts/{id}/history", "/api/v1/admin/device-alerts/{id}/history"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN','STAFF_TECHNICAL')")
    public ApiResponse<List<AlertHistoryResponse>> alertHistory(@PathVariable UUID id) {
        return ApiResponse.ok(alertService.history(id));
    }
}
