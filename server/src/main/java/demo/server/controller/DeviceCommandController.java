package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.common.response.PageResponse;
import demo.server.dto.iot.command.ApplyStationPreferenceResponse;
import demo.server.dto.iot.command.CommandHistoryResponse;
import demo.server.dto.iot.command.DeviceCommandAckRequest;
import demo.server.dto.iot.command.DeviceCommandResponse;
import demo.server.service.iot.DeviceCommandService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class DeviceCommandController {

    private final DeviceCommandService commandService;

    public DeviceCommandController(DeviceCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping({"/api/v1/iot/stations/{stationId}/apply-profile", "/api/v1/stations/{stationId}/apply-profile"})
    @PreAuthorize("hasRole('GAMER')")
    public ApiResponse<ApplyStationPreferenceResponse> applyProfile(@PathVariable UUID stationId,
                                                                    @RequestHeader(value = "X-Correlation-ID", required = false) UUID correlationId) {
        return ApiResponse.ok(commandService.applyPreference(stationId, correlationId), "Station preference applied");
    }

    @PostMapping("/api/v1/admin/devices/{deviceId}/emergency-stop")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN','STAFF_TECHNICAL')")
    public ApiResponse<DeviceCommandResponse> emergencyStop(@PathVariable UUID deviceId) {
        return ApiResponse.ok(commandService.emergencyStop(deviceId), "Emergency stop command issued");
    }

    @PostMapping("/api/v1/iot/commands/ack")
    public ApiResponse<DeviceCommandResponse> acknowledge(@RequestHeader("X-Station-Secret") String stationSecret,
                                                         @Valid @RequestBody DeviceCommandAckRequest request) {
        return ApiResponse.ok(commandService.handleAck(request, stationSecret), "Command ACK accepted");
    }

    @PostMapping("/api/v1/admin/iot/commands/timeouts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN','STAFF_TECHNICAL')")
    public ApiResponse<List<DeviceCommandResponse>> markTimeouts() {
        return ApiResponse.ok(commandService.markTimeouts(), "Command timeouts processed");
    }

    @GetMapping("/api/v1/admin/stations/{stationId}/commands")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN','STAFF_TECHNICAL')")
    public ApiResponse<PageResponse<DeviceCommandResponse>> commands(@PathVariable UUID stationId,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.from(commandService.history(stationId, page, size)));
    }

    @GetMapping("/api/v1/admin/iot/commands/{commandId}/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN','STAFF_TECHNICAL')")
    public ApiResponse<List<CommandHistoryResponse>> commandHistory(@PathVariable UUID commandId) {
        return ApiResponse.ok(commandService.commandHistory(commandId));
    }
}
