package com.parosurvivors.serviya.feedback.application.events;

import com.parosurvivors.serviya.feedback.application.dto.FeedbackSide;

import java.util.List;

/**
 * Publicado cuando se elimina una reseña o se retiran comentario/etiquetas.
 */
public record ReviewDeletedEvent(
        FeedbackSide side,
        Long serviceId,
        Long offererId,
        Long clientId,
        List<FeedbackTagEventItem> tags) {
}
