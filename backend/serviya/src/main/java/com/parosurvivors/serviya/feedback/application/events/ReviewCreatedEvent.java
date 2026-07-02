package com.parosurvivors.serviya.feedback.application.events;

import com.parosurvivors.serviya.feedback.application.dto.FeedbackSide;

import java.util.List;

/**
 * Publicado cuando se crea o amplía una reseña (comentario y/o etiquetas).
 */
public record ReviewCreatedEvent(
        FeedbackSide side,
        Long serviceId,
        Long offererId,
        Long clientId,
        List<FeedbackTagEventItem> tags) {
}
