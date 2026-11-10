package com.parosurvivors.chat.directory.infrastructure.adapters.input;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parosurvivors.chat.directory.application.services.UserProjectionService;
import com.parosurvivors.chat.shared.infrastructure.messaging.ChatMessagingConfig;
import com.parosurvivors.chat.shared.infrastructure.messaging.EventEnvelope;
import com.parosurvivors.chat.shared.infrastructure.messaging.ProcessedEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Mantiene {@code user_directory} a partir de los eventos de usuarios (guía §9.2). */
@Component
class UserEventsListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventsListener.class);

    private final UserProjectionService projection;
    private final ProcessedEvents processed;
    private final ObjectMapper mapper;

    UserEventsListener(UserProjectionService projection, ProcessedEvents processed, ObjectMapper mapper) {
        this.projection = projection;
        this.processed = processed;
        this.mapper = mapper;
    }

    @RabbitListener(queues = ChatMessagingConfig.USERS_QUEUE)
    void onMessage(Message amqp) {
        EventEnvelope event = EventEnvelope.parse(mapper, amqp.getBody());
        MDC.put("correlationId", event.eventId());
        try {
            if (processed.alreadyProcessed(event.eventId())) {
                log.info("Evento {} ya procesado: se ignora", event.eventId());
                return;
            }
            long userId = event.requiredLong("user_id");
            switch (event.eventType()) {
                case ChatMessagingConfig.USER_REGISTERED -> projection.upsertProfile(userId,
                        event.requiredText("display_name"), event.optionalText("avatar_url"), event.occurredAt());
                case ChatMessagingConfig.USER_UPDATED -> projection.upsertProfile(userId,
                        event.optionalText("display_name"), event.optionalText("avatar_url"), event.occurredAt());
                case ChatMessagingConfig.USER_SUSPENDED -> projection.suspend(userId, event.occurredAt());
                case ChatMessagingConfig.USER_REACTIVATED -> projection.reactivate(userId, event.occurredAt());
                case ChatMessagingConfig.USER_DELETED -> projection.delete(userId, event.occurredAt());
                default -> throw new AmqpRejectAndDontRequeueException("Tipo de evento no soportado: " + event.eventType());
            }
            processed.markProcessed(event.eventId(), event.eventType());
        } finally {
            MDC.remove("correlationId");
        }
    }
}
