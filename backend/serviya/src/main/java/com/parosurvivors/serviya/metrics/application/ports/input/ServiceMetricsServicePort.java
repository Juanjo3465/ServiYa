package com.parosurvivors.serviya.metrics.application.ports.input;

import com.parosurvivors.serviya.metrics.domain.ServiceMetrics;

/**
 * Puerto de entrada de ServiceMetricsService. Consulta ({@link #getMetrics}) + escritura disparada
 * por eventos: los adaptadores {@code @TransactionalEventListener} de feedback traducen el evento a
 * los métodos apply* y delegan aquí (el puerto habla en primitivos, nunca importa el evento).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 6).
 */
public interface ServiceMetricsServicePort {

    ServiceMetrics getMetrics(Long serviceId);

    /** Feedback del cliente al servicio: recalcula promedio (si hay rating) e incrementa comentarios (si hay). */
    void applyFeedbackSubmitted(Long serviceId, Integer rating, boolean hasComment);

    /** Reverso del feedback del cliente al servicio: decrementa/recalcula, sin bajar de 0. */
    void applyFeedbackReverted(Long serviceId, Integer rating, boolean hasComment);
}
