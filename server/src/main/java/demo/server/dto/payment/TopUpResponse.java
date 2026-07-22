package demo.server.dto.payment;

import demo.server.common.enums.PaymentTransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TopUpResponse(
        UUID paymentTransactionId,
        String provider,
        String providerTransactionId,
        PaymentTransactionStatus status,
        BigDecimal amount,
        String currency,
        String checkoutUrl,
        String adapterMode,
        Instant requestedAt
) {
}
