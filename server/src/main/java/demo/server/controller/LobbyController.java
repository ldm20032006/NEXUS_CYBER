package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.dto.lobby.CreateLobbyRequest;
import demo.server.dto.lobby.LobbyMessageRequest;
import demo.server.dto.lobby.LobbyMessageResponse;
import demo.server.dto.lobby.LobbyResponse;
import demo.server.dto.lobby.VoiceTokenResponse;
import demo.server.dto.lobby.VoiceWebhookResponse;
import demo.server.service.lfg.LfgLobbyService;
import demo.server.service.lfg.VoiceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lobbies")
@PreAuthorize("isAuthenticated()")
public class LobbyController {

    private final LfgLobbyService service;
    private final VoiceService voiceService;

    public LobbyController(LfgLobbyService service, VoiceService voiceService) {
        this.service = service;
        this.voiceService = voiceService;
    }

    @PostMapping
    public ApiResponse<LobbyResponse> create(@Valid @RequestBody CreateLobbyRequest request) {
        return ApiResponse.ok(service.createLobby(request), "Lobby created");
    }

    @GetMapping("/{id}")
    public ApiResponse<LobbyResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.getLobby(id));
    }

    @DeleteMapping("/{id}/members/me")
    public ApiResponse<LobbyResponse> leave(@PathVariable UUID id) {
        return ApiResponse.ok(service.leave(id), "Left lobby");
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ApiResponse<LobbyResponse> kick(@PathVariable UUID id, @PathVariable UUID userId) {
        return ApiResponse.ok(service.kick(id, userId), "Lobby member removed");
    }

    @PostMapping("/{id}/leader/{userId}")
    public ApiResponse<LobbyResponse> transfer(@PathVariable UUID id, @PathVariable UUID userId) {
        return ApiResponse.ok(service.transfer(id, userId), "Lobby leader transferred");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> disband(@PathVariable UUID id) {
        service.disband(id);
        return ApiResponse.ok(null, "Lobby disbanded");
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<LobbyMessageResponse> sendMessage(@PathVariable UUID id,
                                                         @Valid @RequestBody LobbyMessageRequest request) {
        return ApiResponse.ok(service.sendMessage(id, request), "Lobby message sent");
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<List<LobbyMessageResponse>> messages(@PathVariable UUID id,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.messages(id, page, size));
    }

    @PostMapping("/{id}/voice/token")
    public ApiResponse<VoiceTokenResponse> voiceToken(@PathVariable UUID id) {
        VoiceTokenResponse response = voiceService.issueToken(id);
        return ApiResponse.ok(response, response.token() == null ? "VOICE_UNAVAILABLE" : "Voice token issued");
    }

    @PostMapping("/voice/webhooks/mock")
    @PreAuthorize("permitAll()")
    public ApiResponse<VoiceWebhookResponse> voiceWebhook(@RequestBody String rawBody,
                                                          @RequestHeader("X-Voice-Timestamp") String timestamp,
                                                          @RequestHeader("X-Voice-Signature") String signature) {
        return ApiResponse.ok(voiceService.handleWebhook(rawBody, timestamp, signature), "Voice webhook processed");
    }
}
