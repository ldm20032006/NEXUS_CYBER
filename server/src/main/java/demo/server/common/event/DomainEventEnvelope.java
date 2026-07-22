package demo.server.common.event;

import java.time.Instant;
import java.util.UUID;

public record DomainEventEnvelope<T>(
        UUID eventId,
        String eventType,
        int version,
        Instant timestamp,
        String correlationId,
        T payload
) {
    public DomainEventEnvelope {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp is required");
        }
    }
}
