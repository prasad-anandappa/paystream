package com.prasad.paystream.events.transport;

import com.prasad.paystream.events.domain.OutboxEvent;

/**
 * Where published events go. The default implementation logs them; a Kafka or
 * HTTP-webhook transport drops in behind this interface without touching the
 * relay or the core.
 */
public interface EventTransport {

    void send(OutboxEvent event) throws Exception;
}
