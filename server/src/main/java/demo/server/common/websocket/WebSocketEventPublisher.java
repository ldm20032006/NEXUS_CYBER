package demo.server.common.websocket;

import demo.server.common.event.DomainEventEnvelope;
import demo.server.common.event.DomainEventEnvelopeFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class WebSocketEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final DomainEventEnvelopeFactory envelopeFactory;

    public WebSocketEventPublisher(SimpMessagingTemplate messagingTemplate, DomainEventEnvelopeFactory envelopeFactory) {
        this.messagingTemplate = messagingTemplate;
        this.envelopeFactory = envelopeFactory;
    }

    public <T> DomainEventEnvelope<T> send(String destination, String eventType, int version, T payload) {
        DomainEventEnvelope<T> envelope = envelopeFactory.create(eventType, version, payload);
        messagingTemplate.convertAndSend(destination, envelope);
        return envelope;
    }

    public <T> DomainEventEnvelope<T> sendAfterCommit(String destination, String eventType, int version, T payload) {
        DomainEventEnvelope<T> envelope = envelopeFactory.create(eventType, version, payload);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            messagingTemplate.convertAndSend(destination, envelope);
            return envelope;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend(destination, envelope);
            }
        });
        return envelope;
    }

    public <T> DomainEventEnvelope<T> sendToUser(String userName, String destination, String eventType, int version, T payload) {
        DomainEventEnvelope<T> envelope = envelopeFactory.create(eventType, version, payload);
        messagingTemplate.convertAndSendToUser(userName, destination, envelope);
        return envelope;
    }
}
