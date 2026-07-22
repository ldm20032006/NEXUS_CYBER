package demo.server.common.response;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        String correlationId,
        List<FieldViolation> violations
) {
}
