package demo.server.dto.session;

import demo.server.common.enums.SessionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlaySessionResponse(
        UUID id,
        UUID userId,
        UUID stationId,
        UUID qrSessionId,
        SessionStatus status,
        Instant startedAt,
        Instant endedAt,
        Integer durationMinutes,
        BigDecimal estimatedCost,
        BigDecimal actualCost,
        BigDecimal startBalance,
        BigDecimal endBalance,
        String endedReason
) {
}
