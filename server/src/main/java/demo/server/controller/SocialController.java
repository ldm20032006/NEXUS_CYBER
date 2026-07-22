package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.dto.gamer.PublicGamerProfileResponse;
import demo.server.dto.social.BlockUserRequest;
import demo.server.dto.social.ReportUserRequest;
import demo.server.dto.social.UserBlockResponse;
import demo.server.dto.social.UserReportResponse;
import demo.server.service.social.SocialModerationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/social")
@PreAuthorize("isAuthenticated()")
public class SocialController {

    private final SocialModerationService service;

    public SocialController(SocialModerationService service) {
        this.service = service;
    }

    @PostMapping("/blocks/{userId}")
    public ApiResponse<UserBlockResponse> block(@PathVariable UUID userId,
                                                @Valid @RequestBody(required = false) BlockUserRequest request) {
        return ApiResponse.ok(service.block(userId, request), "User blocked");
    }

    @DeleteMapping("/blocks/{userId}")
    public ApiResponse<Void> unblock(@PathVariable UUID userId) {
        service.unblock(userId);
        return ApiResponse.ok(null, "User unblocked");
    }

    @GetMapping("/blocks")
    public ApiResponse<List<UserBlockResponse>> blocks() {
        return ApiResponse.ok(service.myBlocks());
    }

    @PostMapping("/reports/{userId}")
    public ApiResponse<UserReportResponse> report(@PathVariable UUID userId,
                                                  @Valid @RequestBody ReportUserRequest request) {
        return ApiResponse.ok(service.report(userId, request), "User reported");
    }

    @GetMapping("/reports/me")
    public ApiResponse<List<UserReportResponse>> myReports() {
        return ApiResponse.ok(service.myReports());
    }

    @GetMapping("/radar/users")
    public ApiResponse<List<PublicGamerProfileResponse>> radar(@RequestParam UUID branchId) {
        return ApiResponse.ok(service.radar(branchId));
    }
}
