package demo.server.common.event;

public interface DomainEventPublisher {

    void publish(Object event);

    void publishAfterCommit(Object event);
}
