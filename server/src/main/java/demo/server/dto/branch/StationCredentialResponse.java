package demo.server.dto.branch;

import java.time.Instant;
import java.util.UUID;

public record StationCredentialResponse(
        UUID credentialId,
        UUID stationId,
        String secret,
        Instant issuedAt,
        Instant expiresAt
) {
}
