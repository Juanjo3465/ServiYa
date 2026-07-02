package com.parosurvivors.serviya.feedback.application.ports.input;

import com.parosurvivors.serviya.feedback.application.dto.command.SubmitServiceFeedbackCommand;
import com.parosurvivors.serviya.feedback.application.dto.result.ServiceFeedbackResult;
import com.parosurvivors.serviya.feedback.domain.ServiceFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Puerto de entrada de ServiceFeedbackService — fachada del feedback del cliente al servicio.
 * Recibe Command; las lecturas devuelven Result (vista agregada rating+resena). Nunca tipos web.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 5).
 */
public interface ServiceFeedbackServicePort {

    void submitServiceFeedback(SubmitServiceFeedbackCommand command);

    ServiceFeedbackResult getServiceFeedback(Long requestId);

    Page<ServiceFeedbackResult> getServiceFeedbackList(Long serviceId, Pageable pageable);

    Page<ServiceFeedbackResult> getServiceFeedbackByClient(Long clientId, Pageable pageable);

    /**
     * Reseñas (feedback con comentario) más recientes de un servicio, hasta {@code limit}.
     * Devuelve el dominio {@link ServiceFeedback} (no Result) para que el detalle del servicio
     * pueda emparejar cada reseña con el perfil de su autor.
     */
    List<ServiceFeedback> getRecentServiceFeedback(Long serviceId, int limit);

    boolean revertFeedback(Long requestId);

    void requireRequestPartyAccess(Long viewerId, Long requestId);

    void requireClientFeedbackAccess(Long viewerId, Long clientId);
}
