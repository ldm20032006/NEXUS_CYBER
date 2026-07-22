package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.dto.lfg.TeamInvitationRequest;
import demo.server.dto.lfg.TeamInvitationResponse;
import demo.server.dto.lobby.LobbyResponse;
import demo.server.service.lfg.LfgLobbyService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/team-invitations")
@PreAuthorize("isAuthenticated()")
public class TeamInvitationController {

    private final LfgLobbyService service;

    public TeamInvitationController(LfgLobbyService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<TeamInvitationResponse> invite(@Valid @RequestBody TeamInvitationRequest request) {
        return ApiResponse.ok(service.invite(request), "Team invitation sent");
    }

    @GetMapping("/received")
    public ApiResponse<List<TeamInvitationResponse>> received() {
        return ApiResponse.ok(service.receivedInvitations());
    }

    @GetMapping("/sent")
    public ApiResponse<List<TeamInvitationResponse>> sent() {
        return ApiResponse.ok(service.sentInvitations());
    }

    @PatchMapping("/{id}/accept")
    public ApiResponse<LobbyResponse> accept(@PathVariable UUID id) {
        return ApiResponse.ok(service.acceptInvitation(id), "Team invitation accepted");
    }

    @PatchMapping("/{id}/reject")
    public ApiResponse<TeamInvitationResponse> reject(@PathVariable UUID id) {
        return ApiResponse.ok(service.rejectInvitation(id), "Team invitation rejected");
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<TeamInvitationResponse> cancel(@PathVariable UUID id) {
        return ApiResponse.ok(service.cancelInvitation(id), "Team invitation cancelled");
    }
}
