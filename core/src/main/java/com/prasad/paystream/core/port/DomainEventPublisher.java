package com.prasad.paystream.core.port;

/**
 * Outbound port (hexagonal architecture): the core announces domain events
 * without knowing how they are delivered. The events module provides the
 * transactional-outbox adapter; in core-only tests no adapter is present and
 * events are simply not recorded.
 */
public interface DomainEventPublisher {

    void publish(String aggregateType, String aggregateId, String eventType, String payload);
}
