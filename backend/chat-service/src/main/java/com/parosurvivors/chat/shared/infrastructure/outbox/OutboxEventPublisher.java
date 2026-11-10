package com.parosurvivors.chat.shared.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parosurvivors.chat.shared.application.ports.EventPublisher;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/** Arma el sobre común de la guía §9.1 y lo deja en el outbox (ADR-009), en la transacción del llamador. */
@Component
public class OutboxEventPublisher implements EventPublisher {

    public static final String PRODUCER = "chat-service";
    public static final String CORRELATION_MDC_KEY = "correlationId";

    private final OutboxWriter writer;
    private final ObjectMapper mapper;
    private final Clock clock;

    OutboxEventPublisher(OutboxWriter writer, ObjectMapper mapper, Clock clock) {
        this.writer = writer;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public void publish(String exchange, String routingKey, Map<String, Object> data) {
        String eventId = UUID.randomUUID().toString();
        String correlationId = MDC.get(CORRELATION_MDC_KEY);
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("event_id", eventId);
        envelope.put("event_type", routingKey);
        envelope.put("event_version", 1);
        envelope.put("occurred_at", clock.instant().toString());
        envelope.put("producer", PRODUCER);
        envelope.put("correlation_id", correlationId != null ? correlationId : UUID.randomUUID().toString());
        envelope.put("data", data);
        try {
            writer.insert(eventId, exchange, routingKey, mapper.writeValueAsString(envelope));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento " + routingKey, e);
        }
    }
}
