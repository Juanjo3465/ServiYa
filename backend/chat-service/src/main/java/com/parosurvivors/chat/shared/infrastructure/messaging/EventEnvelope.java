package com.parosurvivors.chat.shared.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

/**
 * Sobre común de eventos entrantes (guía §9.1). Todo error de forma es PERMANENTE: reintentar no lo arregla, así
 * que se rechaza sin reencolar y el mensaje va a la DLQ.
 */
public record EventEnvelope(String eventId, String eventType, Instant occurredAt, JsonNode data) {

    public static EventEnvelope parse(ObjectMapper mapper, byte[] body) {
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (java.io.IOException e) { // incluye JsonProcessingException
            throw new AmqpRejectAndDontRequeueException("Evento con JSON inválido", e);
        }
        String eventId = text(root, "event_id");
        String eventType = text(root, "event_type");
        String occurredAt = text(root, "occurred_at");
        if (eventId == null || eventType == null || occurredAt == null || !root.path("data").isObject()) {
            throw new AmqpRejectAndDontRequeueException(
                    "Evento sin event_id, event_type, occurred_at o data: " + eventId);
        }
        try {
            return new EventEnvelope(eventId, eventType, Instant.parse(occurredAt), root.path("data"));
        } catch (DateTimeParseException e) {
            throw new AmqpRejectAndDontRequeueException("occurred_at inválido en el evento " + eventId, e);
        }
    }

    public long requiredLong(String field) {
        JsonNode v = data.path(field);
        if (!v.isIntegralNumber()) {
            throw new AmqpRejectAndDontRequeueException(
                    "El evento " + eventId + " no trae el campo numérico «" + field + "»");
        }
        return v.asLong();
    }

    public String requiredText(String field) {
        String v = text(data, field);
        if (v == null || v.isBlank()) {
            throw new AmqpRejectAndDontRequeueException(
                    "El evento " + eventId + " no trae el campo «" + field + "»");
        }
        return v;
    }

    /** Texto opcional: ausente → null (distinto de cadena vacía, que significa «borrar»). */
    public String optionalText(String field) {
        return text(data, field);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isTextual() ? v.asText() : null;
    }
}
