package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.dto.payment.PaymentWebhookResponse;
import demo.server.dto.payment.TopUpRequest;
import demo.server.dto.payment.TopUpResponse;
import demo.server.service.payment.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/topups")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TopUpResponse> topUp(@Valid @RequestBody TopUpRequest request,
                                            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResponse.ok(paymentService.startTopUp(request.amount(), idempotencyKey), "Mock payment top-up created");
    }

    @PostMapping("/webhooks/mock")
    public ApiResponse<PaymentWebhookResponse> mockWebhook(@RequestBody String rawBody,
                                                           @RequestHeader("X-Payment-Timestamp") String timestamp,
                                                           @RequestHeader("X-Payment-Signature") String signature) {
        return ApiResponse.ok(paymentService.handleWebhook(rawBody, timestamp, signature), "Payment webhook processed");
    }
}
