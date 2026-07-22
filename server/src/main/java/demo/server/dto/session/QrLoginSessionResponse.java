package demo.server.dto.session;

import demo.server.common.enums.QrLoginSessionStatus;

import java.time.Instant;
import java.util.UUID;

public record QrLoginSessionResponse(
        UUID qrSessionId,
        UUID stationId,
        String nonce,
        String qrPayload,
        Instant expiresAt,
        QrLoginSessionStatus status
) {
}
