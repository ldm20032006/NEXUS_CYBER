package demo.server.dto.payment;

import demo.server.common.enums.PaymentTransactionStatus;

import java.util.UUID;

public record PaymentWebhookResponse(
        UUID paymentTransactionId,
        String providerTransactionId,
        PaymentTransactionStatus status,
        boolean walletCredited
) {
}
