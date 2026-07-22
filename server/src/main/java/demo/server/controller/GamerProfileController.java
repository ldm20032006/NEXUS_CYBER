package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.dto.gamer.GamerGameProfileRequest;
import demo.server.dto.gamer.GamerGameProfileResponse;
import demo.server.dto.gamer.GamerProfileRequest;
import demo.server.dto.gamer.PublicGamerProfileResponse;
import demo.server.dto.gamer.StationPreferenceRequest;
import demo.server.dto.gamer.StationPreferenceResponse;
import demo.server.service.gamer.GamerProfileService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles/me")
@PreAuthorize("isAuthenticated()")
public class GamerProfileController {

    private final GamerProfileService gamerProfileService;

    public GamerProfileController(GamerProfileService gamerProfileService) {
        this.gamerProfileService = gamerProfileService;
    }

    @GetMapping
    public ApiResponse<PublicGamerProfileResponse> me() {
        return ApiResponse.ok(gamerProfileService.me());
    }

    @PutMapping
    public ApiResponse<PublicGamerProfileResponse> updateMe(@Valid @RequestBody GamerProfileRequest request) {
        return ApiResponse.ok(gamerProfileService.updateMe(request), "Profile updated");
    }

    @PostMapping("/avatar")
    public ApiResponse<PublicGamerProfileResponse> updateAvatar(@Valid @RequestBody GamerProfileRequest request) {
        return ApiResponse.ok(gamerProfileService.updateMe(request), "Avatar updated");
    }

    @GetMapping({"/game-profiles", "/games"})
    public ApiResponse<List<GamerGameProfileResponse>> gameProfiles() {
        return ApiResponse.ok(gamerProfileService.gameProfiles());
    }

    @PostMapping({"/game-profiles", "/games"})
    public ApiResponse<GamerGameProfileResponse> createGameProfile(@Valid @RequestBody GamerGameProfileRequest request) {
        return ApiResponse.ok(gamerProfileService.createGameProfile(request), "Game profile created");
    }

    @PutMapping("/game-profiles/{id}")
    public ApiResponse<GamerGameProfileResponse> updateGameProfile(@PathVariable UUID id, @Valid @RequestBody GamerGameProfileRequest request) {
        return ApiResponse.ok(gamerProfileService.updateGameProfile(id, request), "Game profile updated");
    }

    @DeleteMapping("/game-profiles/{id}")
    public ApiResponse<Void> deleteGameProfile(@PathVariable UUID id) {
        gamerProfileService.deleteGameProfile(id);
        return ApiResponse.ok(null, "Game profile deleted");
    }

    @GetMapping("/station-preference")
    public ApiResponse<StationPreferenceResponse> stationPreference() {
        return ApiResponse.ok(gamerProfileService.stationPreference());
    }

    @PutMapping("/station-preference")
    public ApiResponse<StationPreferenceResponse> updateStationPreference(@Valid @RequestBody StationPreferenceRequest request) {
        return ApiResponse.ok(gamerProfileService.updateStationPreference(request), "Station preference updated");
    }
}
