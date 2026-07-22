package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.dto.wallet.AdminAdjustmentRequest;
import demo.server.dto.wallet.RefundRequest;
import demo.server.dto.wallet.WalletResponse;
import demo.server.dto.wallet.WalletTransactionResponse;
import demo.server.service.wallet.WalletService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/api/v1/wallets/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WalletResponse> currentWallet() {
        return ApiResponse.ok(walletService.currentWallet());
    }

    @GetMapping("/api/v1/wallets/me/transactions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<WalletTransactionResponse>> currentTransactions() {
        return ApiResponse.ok(walletService.currentTransactions());
    }

    @PostMapping("/api/v1/admin/wallets/{userId}/adjustments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN')")
    public ApiResponse<WalletTransactionResponse> adjust(@PathVariable UUID userId,
                                                         @Valid @RequestBody AdminAdjustmentRequest request,
                                                         @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResponse.ok(walletService.adminAdjustment(userId, request.amount(), request.reason(), idempotencyKey),
                "Wallet adjusted");
    }

    @PostMapping("/api/v1/admin/wallet-transactions/{transactionId}/refund")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN')")
    public ApiResponse<WalletTransactionResponse> refund(@PathVariable UUID transactionId,
                                                         @Valid @RequestBody RefundRequest request,
                                                         @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResponse.ok(walletService.refund(transactionId, request.reason(), idempotencyKey),
                "Wallet transaction refunded");
    }
}
