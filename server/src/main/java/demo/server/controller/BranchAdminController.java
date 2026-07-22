package demo.server.controller;

import demo.server.common.enums.BranchStatus;
import demo.server.common.enums.StationStatus;
import demo.server.common.enums.ZoneStatus;
import demo.server.common.response.ApiResponse;
import demo.server.common.response.PageResponse;
import demo.server.dto.branch.BranchRequest;
import demo.server.dto.branch.BranchResponse;
import demo.server.dto.branch.StationCredentialResponse;
import demo.server.dto.branch.StationRequest;
import demo.server.dto.branch.StationResponse;
import demo.server.dto.branch.ZoneRequest;
import demo.server.dto.branch.ZoneResponse;
import demo.server.service.branch.BranchStationService;
import jakarta.validation.Valid;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN')")
public class BranchAdminController {

    private final BranchStationService branchStationService;

    public BranchAdminController(BranchStationService branchStationService) {
        this.branchStationService = branchStationService;
    }

    @PostMapping("/branches")
    public ApiResponse<BranchResponse> createBranch(@Valid @RequestBody BranchRequest request) {
        return ApiResponse.ok(branchStationService.createBranch(request), "Branch created");
    }

    @GetMapping("/branches")
    public ApiResponse<PageResponse<BranchResponse>> listBranches(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) BranchStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(branchStationService.listBranches(code, status, page, size));
    }

    @PutMapping("/branches/{id}")
    public ApiResponse<BranchResponse> updateBranch(@PathVariable UUID id, @Valid @RequestBody BranchRequest request) {
        return ApiResponse.ok(branchStationService.updateBranch(id, request), "Branch updated");
    }

    @DeleteMapping("/branches/{id}")
    public ApiResponse<Void> deleteBranch(@PathVariable UUID id) {
        branchStationService.deleteBranch(id);
        return ApiResponse.ok(null, "Branch deleted");
    }

    @PostMapping("/zones")
    public ApiResponse<ZoneResponse> createZone(@Valid @RequestBody ZoneRequest request) {
        return ApiResponse.ok(branchStationService.createZone(request), "Zone created");
    }

    @GetMapping("/zones")
    public ApiResponse<PageResponse<ZoneResponse>> listZones(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) ZoneStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(branchStationService.listZones(branchId, status, page, size));
    }

    @PutMapping("/zones/{id}")
    public ApiResponse<ZoneResponse> updateZone(@PathVariable UUID id, @Valid @RequestBody ZoneRequest request) {
        return ApiResponse.ok(branchStationService.updateZone(id, request), "Zone updated");
    }

    @DeleteMapping("/zones/{id}")
    public ApiResponse<Void> deleteZone(@PathVariable UUID id) {
        branchStationService.deleteZone(id);
        return ApiResponse.ok(null, "Zone deleted");
    }

    @PostMapping("/stations")
    public ApiResponse<StationResponse> createStation(@Valid @RequestBody StationRequest request) {
        return ApiResponse.ok(branchStationService.createStation(request), "Station created");
    }

    @GetMapping("/stations")
    public ApiResponse<PageResponse<StationResponse>> listStations(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) StationStatus status,
            @RequestParam(required = false) String code,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(branchStationService.listStations(branchId, zoneId, status, code, page, size));
    }

    @PatchMapping("/stations/{id}")
    public ApiResponse<StationResponse> updateStation(@PathVariable UUID id, @Valid @RequestBody StationRequest request) {
        return ApiResponse.ok(branchStationService.updateStation(id, request), "Station updated");
    }

    @DeleteMapping("/stations/{id}")
    public ApiResponse<Void> deleteStation(@PathVariable UUID id) {
        branchStationService.deleteStation(id);
        return ApiResponse.ok(null, "Station deleted");
    }

    @PostMapping("/stations/{id}/credentials")
    public ApiResponse<StationCredentialResponse> createCredential(@PathVariable UUID id) {
        return ApiResponse.ok(branchStationService.createCredential(id), "Station credential created");
    }

    @PostMapping("/stations/{id}/credentials/rotate")
    public ApiResponse<StationCredentialResponse> rotateCredential(@PathVariable UUID id) {
        return ApiResponse.ok(branchStationService.rotateCredential(id), "Station credential rotated");
    }

    @PostMapping("/stations/{id}/credentials/revoke")
    public ApiResponse<Void> revokeCredential(@PathVariable UUID id) {
        branchStationService.revokeCredential(id);
        return ApiResponse.ok(null, "Station credential revoked");
    }
}
