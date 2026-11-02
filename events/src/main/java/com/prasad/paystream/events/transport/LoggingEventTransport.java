package com.prasad.paystream.events.transport;

import com.prasad.paystream.events.domain.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Default transport: writes the event to the application log. */
@Component
public class LoggingEventTransport implements EventTransport {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventTransport.class);

    @Override
    public void send(OutboxEvent event) {
        log.info("OUTBOX PUBLISH {} {}:{} {}",
                event.getEventType(), event.getAggregateType(), event.getAggregateId(), event.getPayload());
    }
}
