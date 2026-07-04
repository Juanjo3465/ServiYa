package com.parosurvivors.serviya.metrics.application.ports.input;

import com.parosurvivors.serviya.metrics.domain.OffererMetrics;
import com.parosurvivors.serviya.requests.domain.RequestStatus;

/**
 * Puerto de entrada de OffererMetricsService. Consulta (devuelve dominio; el resumen lo arma el
 * WebMapper) + escritura por eventos: escucha el feedback de servicio (rating/comentarios/tags del
 * oferente dueño) y los cambios de estado de solicitud. Los adaptadores traducen el evento a estos
 * métodos y delegan. Ver documents/project-structure/estructura-servicios.docx (módulo 6).
 */
public interface OffererMetricsServicePort {

    OffererMetrics getAllMetrics(Long offererId);

    OffererMetrics getMainMetrics(Long offererId);

    /** Feedback del cliente a un servicio del oferente: recalcula promedio, comentarios y tags +/-. */
    void applyServiceFeedbackSubmitted(Long offererId, Integer rating, boolean hasComment, int positiveTags, int negativeTags);

    /** Reverso del feedback de servicio: decrementa/recalcula, sin bajar de 0. */
    void applyServiceFeedbackReverted(Long offererId, Integer rating, boolean hasComment, int positiveTags, int negativeTags);

    /**
     * Incrementa el contador del nuevo estado de la solicitud para el oferente
     * (accepted/completed/cancelled/not_provided). RESCHEDULED NO cuenta para el oferente:
     * el que reprograma es el cliente; la participación del oferente se cuenta como propuestas enviadas.
     */
    void applyRequestStatusChanged(Long offererId, RequestStatus newStatus);

    /** Solicitud original recibida por el oferente (evento de creación, no reprogramaciones). */
    void incrementRequestsReceived(Long offererId);

    /** Propuesta de reprogramación enviada por el oferente. */
    void incrementRescheduleProposalsSent(Long offererId);
}
