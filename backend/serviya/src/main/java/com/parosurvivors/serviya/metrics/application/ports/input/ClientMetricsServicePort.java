package com.parosurvivors.serviya.metrics.application.ports.input;

import com.parosurvivors.serviya.metrics.domain.ClientMetrics;
import com.parosurvivors.serviya.requests.domain.RequestStatus;

/**
 * Puerto de entrada de ClientMetricsService. Consulta (devuelve dominio; el resumen lo arma el
 * WebMapper) + escritura por eventos: escucha el feedback del oferente al cliente y los cambios de
 * estado de las solicitudes del cliente. Los adaptadores traducen el evento a estos métodos y delegan.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 6).
 */
public interface ClientMetricsServicePort {

    ClientMetrics getAllMetrics(Long clientId);

    ClientMetrics getMainMetrics(Long clientId);

    /** Feedback del oferente al cliente: recalcula promedio, comentarios y tags +/-. */
    void applyClientFeedbackSubmitted(Long clientId, Integer rating, boolean hasComment, int positiveTags, int negativeTags);

    /** Reverso del feedback al cliente: decrementa/recalcula, sin bajar de 0. */
    void applyClientFeedbackReverted(Long clientId, Integer rating, boolean hasComment, int positiveTags, int negativeTags);

    /** Incrementa el contador del nuevo estado de la solicitud del cliente (accepted/completed/cancelled/rescheduled/not_provided). */
    void applyRequestStatusChanged(Long clientId, RequestStatus newStatus);

    /** Solicitud original enviada por el cliente (evento de creación, no reprogramaciones). */
    void incrementRequestsSent(Long clientId);
}
