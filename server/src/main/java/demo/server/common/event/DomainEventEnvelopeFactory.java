package demo.server.common.event;

import demo.server.common.logging.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class DomainEventEnvelopeFactory {

    public <T> DomainEventEnvelope<T> create(String eventType, int version, T payload) {
        return new DomainEventEnvelope<>(
                UUID.randomUUID(),
                eventType,
                version,
                Instant.now(),
                MDC.get(CorrelationIdFilter.MDC_KEY),
                payload);
    }
}
