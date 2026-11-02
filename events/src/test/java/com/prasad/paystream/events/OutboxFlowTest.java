package com.prasad.paystream.events;

import com.prasad.paystream.core.domain.AccountType;
import com.prasad.paystream.core.service.LedgerService;
import com.prasad.paystream.core.service.TransferService;
import com.prasad.paystream.events.domain.OutboxEvent;
import com.prasad.paystream.events.domain.OutboxStatus;
import com.prasad.paystream.events.repository.OutboxEventRepository;
import com.prasad.paystream.events.transport.EventTransport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OutboxFlowTest {

    /** Capturing transport replaces the logging one for assertions. */
    static final List<OutboxEvent> SENT = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class CapturingTransportConfig {
        @Bean
        @Primary
        EventTransport capturingTransport() {
            return SENT::add;
        }
    }

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private OutboxPublisherService publisherService;

    @Test
    void transferWritesAPendingOutboxEventInTheSameFlow() {
        var funding = ledgerService.createAccount("Funding", AccountType.ASSET, "USD", true);
        var alice = ledgerService.createAccount("Alice", AccountType.LIABILITY, "USD", false);

        transferService.transfer("evt-key-1", funding.getId(), alice.getId(), 1_000, "USD");

        List<OutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(1);
        OutboxEvent event = events.get(0);
        assertThat(event.getEventType()).isEqualTo("transfer.completed");
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getPayload()).contains("\"amountMinor\":1000");
    }

    @Test
    void relayPublishesPendingEventsAndMarksThemPublished() {
        var funding = ledgerService.createAccount("Funding2", AccountType.ASSET, "USD", true);
        var bob = ledgerService.createAccount("Bob", AccountType.LIABILITY, "USD", false);
        transferService.transfer("evt-key-2", funding.getId(), bob.getId(), 2_000, "USD");
        SENT.clear();

        int published = publisherService.publishPending();

        assertThat(published).isEqualTo(1);
        assertThat(SENT).hasSize(1);
        OutboxEvent stored = outboxRepository.findAll().get(0);
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(stored.getPublishedAt()).isNotNull();

        // A second pass has nothing left to do — no duplicates.
        assertThat(publisherService.publishPending()).isZero();
    }
}
