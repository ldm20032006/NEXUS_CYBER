package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.dto.branch.StationHeartbeatResponse;
import demo.server.service.branch.BranchStationService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stations")
public class StationKioskController {

    private final BranchStationService branchStationService;

    public StationKioskController(BranchStationService branchStationService) {
        this.branchStationService = branchStationService;
    }

    @PostMapping("/{id}/heartbeat")
    public ApiResponse<StationHeartbeatResponse> heartbeat(
            @PathVariable UUID id,
            @RequestHeader(name = "X-Station-Secret", required = false) String stationSecret
    ) {
        return ApiResponse.ok(branchStationService.heartbeat(id, stationSecret), "Heartbeat accepted");
    }
}
