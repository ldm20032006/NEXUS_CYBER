package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.dto.lfg.LfgSearchRequest;
import demo.server.dto.lfg.LfgSignalRequest;
import demo.server.dto.lfg.LfgSignalResponse;
import demo.server.service.lfg.LfgLobbyService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lfg/signals")
@PreAuthorize("isAuthenticated()")
public class LfgController {

    private final LfgLobbyService service;

    public LfgController(LfgLobbyService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<LfgSignalResponse> create(@Valid @RequestBody LfgSignalRequest request) {
        return ApiResponse.ok(service.createSignal(request), "LFG signal created");
    }

    @GetMapping
    public ApiResponse<List<LfgSignalResponse>> search(@RequestParam(required = false) UUID branchId,
                                                       @RequestParam(required = false) UUID gameId,
                                                       @RequestParam(required = false) UUID rankId,
                                                       @RequestParam(required = false) UUID roleId,
                                                       @RequestParam(required = false) UUID zoneId) {
        return ApiResponse.ok(service.search(new LfgSearchRequest(branchId, gameId, rankId, roleId, zoneId)));
    }

    @GetMapping("/me")
    public ApiResponse<List<LfgSignalResponse>> mine() {
        return ApiResponse.ok(service.mySignals());
    }

    @PutMapping("/{id}")
    public ApiResponse<LfgSignalResponse> update(@PathVariable UUID id, @Valid @RequestBody LfgSignalRequest request) {
        service.cancelSignal(id);
        return ApiResponse.ok(service.createSignal(request), "LFG signal updated");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> cancel(@PathVariable UUID id) {
        service.cancelSignal(id);
        return ApiResponse.ok(null, "LFG signal cancelled");
    }

    @PostMapping("/{id}/renew")
    public ApiResponse<LfgSignalResponse> renew(@PathVariable UUID id) {
        return ApiResponse.ok(service.renewSignal(id), "LFG signal renewed");
    }
}
