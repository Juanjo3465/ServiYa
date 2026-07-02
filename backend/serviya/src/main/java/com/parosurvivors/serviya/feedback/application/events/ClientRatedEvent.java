package com.parosurvivors.serviya.feedback.application.events;

/**
 * Publicado cuando cambia la calificación numérica del oferente a un cliente.
 */
public record ClientRatedEvent(
        Long clientId,
        Long offererId,
        Integer newRating,
        Integer previousRating) {
}
