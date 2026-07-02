package com.parosurvivors.serviya.feedback.application.events;

import com.parosurvivors.serviya.feedback.domain.TagSentiment;

/**
 * Etiqueta incluida en eventos de reseña para actualizar métricas de tags.
 */
public record FeedbackTagEventItem(Long tagId, TagSentiment sentiment) {
}
