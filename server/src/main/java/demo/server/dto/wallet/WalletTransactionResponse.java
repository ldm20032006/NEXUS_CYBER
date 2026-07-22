package demo.server.dto.wallet;

import demo.server.common.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletTransactionResponse(
        UUID id,
        UUID walletId,
        UUID userId,
        TransactionType type,
        BigDecimal amount,
        String currency,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String referenceType,
        String referenceId,
        UUID originalTransactionId,
        String description,
        Instant createdAt
) {
}
