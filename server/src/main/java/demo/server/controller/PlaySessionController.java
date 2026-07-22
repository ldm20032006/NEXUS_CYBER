package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.dto.session.EndSessionRequest;
import demo.server.dto.session.PlaySessionResponse;
import demo.server.service.session.QrSessionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@PreAuthorize("isAuthenticated()")
public class PlaySessionController {

    private final QrSessionService qrSessionService;

    public PlaySessionController(QrSessionService qrSessionService) {
        this.qrSessionService = qrSessionService;
    }

    @GetMapping("/current")
    public ApiResponse<PlaySessionResponse> current() {
        return ApiResponse.ok(qrSessionService.current().orElse(null));
    }

    @GetMapping("/history")
    public ApiResponse<List<PlaySessionResponse>> history() {
        return ApiResponse.ok(qrSessionService.history());
    }

    @PostMapping("/{id}/end")
    public ApiResponse<PlaySessionResponse> end(@PathVariable UUID id, @Valid @RequestBody EndSessionRequest request) {
        return ApiResponse.ok(qrSessionService.end(id, request.reason()), "Session ended");
    }
}
