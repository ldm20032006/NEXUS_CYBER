package demo.server.dto.payment;

import demo.server.common.enums.PaymentTransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentTransactionResponse(
        UUID id,
        UUID userId,
        String provider,
        String providerTransactionId,
        PaymentTransactionStatus status,
        BigDecimal amount,
        String currency,
        UUID walletTransactionId,
        Instant requestedAt,
        Instant processedAt,
        String failureReason
) {
}
