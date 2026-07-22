package demo.server.audit;

import demo.server.common.event.DomainEventEnvelope;
import demo.server.common.event.DomainEventEnvelopeFactory;
import demo.server.common.event.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DomainEventTests {

    @Autowired
    DomainEventEnvelopeFactory envelopeFactory;

    @Autowired
    DomainEventPublisher publisher;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    CapturedEvents capturedEvents;

    @Test
    void envelopeRequiresImmutableCoreFieldsAndCarriesCorrelationId() {
        MDC.put("correlationId", "corr-event-1");
        try {
            DomainEventEnvelope<Map<String, String>> envelope = envelopeFactory.create("auth.login", 1, Map.of("userId", "u1"));

            assertThat(envelope.eventId()).isNotNull();
            assertThat(envelope.eventType()).isEqualTo("auth.login");
            assertThat(envelope.version()).isEqualTo(1);
            assertThat(envelope.timestamp()).isNotNull();
            assertThat(envelope.correlationId()).isEqualTo("corr-event-1");
            assertThat(envelope.payload()).containsEntry("userId", "u1");
        } finally {
            MDC.remove("correlationId");
        }

        assertThatThrownBy(() -> new DomainEventEnvelope<>(UUID.randomUUID(), "", 1, Instant.now(), null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventType is required");
        assertThatThrownBy(() -> new DomainEventEnvelope<>(UUID.randomUUID(), "auth.login", 0, Instant.now(), null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version must be positive");
    }

    @Test
    void publishAfterCommitPublishesOnlyAfterTransactionCommit() {
        DomainEventEnvelope<Map<String, String>> envelope = envelopeFactory.create("audit.test", 1, Map.of("id", "1"));

        transactionTemplate.executeWithoutResult(status -> {
            publisher.publishAfterCommit(envelope);
            assertThat(capturedEvents.events()).isEmpty();
        });

        assertThat(capturedEvents.events()).contains(envelope);
    }

    @TestConfiguration
    static class EventTestConfig {
        @Bean
        CapturedEvents capturedEvents() {
            return new CapturedEvents();
        }

        @Bean
        CapturingEventListener domainEventListener(CapturedEvents capturedEvents) {
            return new CapturingEventListener(capturedEvents);
        }
    }

    static class CapturingEventListener {
        private final CapturedEvents capturedEvents;

        CapturingEventListener(CapturedEvents capturedEvents) {
            this.capturedEvents = capturedEvents;
        }

        @EventListener
        void onDomainEvent(DomainEventEnvelope<?> event) {
            capturedEvents.events().add(event);
        }
    }

    static class CapturedEvents {
        private final List<DomainEventEnvelope<?>> events = new ArrayList<>();

        List<DomainEventEnvelope<?>> events() {
            return events;
        }
    }
}
