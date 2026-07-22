package demo.server.controller;

import demo.server.common.enums.UserReportStatus;
import demo.server.common.response.ApiResponse;
import demo.server.dto.social.ModerationActionRequest;
import demo.server.dto.social.UserReportResponse;
import demo.server.service.social.SocialModerationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/moderation")
@PreAuthorize("hasAnyRole('BRANCH_ADMIN','SUPER_ADMIN')")
public class AdminModerationController {

    private final SocialModerationService service;

    public AdminModerationController(SocialModerationService service) {
        this.service = service;
    }

    @GetMapping("/reports")
    public ApiResponse<List<UserReportResponse>> reports(@RequestParam(required = false) UserReportStatus status) {
        return ApiResponse.ok(service.adminReports(status));
    }

    @PatchMapping("/reports/{id}")
    public ApiResponse<UserReportResponse> moderate(@PathVariable UUID id,
                                                    @Valid @RequestBody ModerationActionRequest request) {
        return ApiResponse.ok(service.moderate(id, request), "Moderation action recorded");
    }
}
