package com.parosurvivors.serviya.feedback.application.events;

/**
 * Publicado cuando cambia la calificación numérica del cliente a un servicio.
 * Los listeners de métricas reaccionan a este evento (no se llama a métricas directamente).
 */
public record ServiceRatedEvent(
        Long serviceId,
        Long offererId,
        Integer newRating,
        Integer previousRating) {
}
