package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.dto.session.PlaySessionResponse;
import demo.server.dto.session.QrConfirmRequest;
import demo.server.dto.session.QrLoginSessionResponse;
import demo.server.service.session.QrSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/qr-sessions", "/api/v1/auth/qr-sessions"})
public class QrSessionController {

    private final QrSessionService qrSessionService;

    public QrSessionController(QrSessionService qrSessionService) {
        this.qrSessionService = qrSessionService;
    }

    @PostMapping
    public ApiResponse<QrLoginSessionResponse> createQr(
            @RequestHeader("X-Station-Id") UUID stationId,
            @RequestHeader("X-Station-Secret") String stationSecret,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.ok(qrSessionService.createQr(stationId, stationSecret, idempotencyKey), "QR session created");
    }

    @GetMapping("/{id}")
    public ApiResponse<QrLoginSessionResponse> getQr(@PathVariable UUID id) {
        return ApiResponse.ok(qrSessionService.getQr(id));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PlaySessionResponse> confirm(
            @PathVariable UUID id,
            @Valid @RequestBody QrConfirmRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.ok(qrSessionService.confirm(id, request, idempotencyKey), "Session started");
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @PathVariable UUID id,
            @RequestHeader("X-Station-Secret") String stationSecret
    ) {
        qrSessionService.cancelQr(id, stationSecret);
        return ApiResponse.ok(null, "QR session cancelled");
    }
}
