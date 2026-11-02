package com.prasad.paystream.events;

import com.prasad.paystream.core.port.DomainEventPublisher;
import com.prasad.paystream.events.domain.OutboxEvent;
import com.prasad.paystream.events.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing the core's {@link DomainEventPublisher} port with the
 * transactional outbox table. Because this runs inside the caller's
 * transaction, the event row commits or rolls back together with the state
 * change it announces — the two can never disagree.
 */
@Component
public class OutboxEventPublisher implements DomainEventPublisher {

    private final OutboxEventRepository repository;

    public OutboxEventPublisher(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void publish(String aggregateType, String aggregateId, String eventType, String payload) {
        repository.save(new OutboxEvent(aggregateType, aggregateId, eventType, payload));
    }
}
