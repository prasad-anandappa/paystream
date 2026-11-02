package com.prasad.paystream.events;

import com.prasad.paystream.events.domain.OutboxEvent;
import com.prasad.paystream.events.domain.OutboxStatus;
import com.prasad.paystream.events.repository.OutboxEventRepository;
import com.prasad.paystream.events.transport.EventTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The outbox relay. On a schedule it takes the oldest PENDING events, hands
 * each to the {@link EventTransport}, and only then marks it PUBLISHED. If the
 * transport throws, the event stays PENDING and is retried on the next pass —
 * at-least-once delivery with no event ever silently dropped.
 */
@Service
public class OutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherService.class);
    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository repository;
    private final EventTransport transport;

    public OutboxPublisherService(OutboxEventRepository repository, EventTransport transport) {
        this.repository = repository;
        this.transport = transport;
    }

    @Scheduled(fixedDelayString = "${paystream.outbox.fixed-delay-ms:5000}")
    @Transactional
    public int publishPending() {
        List<OutboxEvent> pending = repository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, BATCH_SIZE));
        int published = 0;
        for (OutboxEvent event : pending) {
            try {
                transport.send(event);
                event.markPublished();
                repository.save(event);
                published++;
            } catch (Exception e) {
                log.warn("Outbox event {} failed to publish, will retry: {}", event.getId(), e.getMessage());
            }
        }
        return published;
    }
}
