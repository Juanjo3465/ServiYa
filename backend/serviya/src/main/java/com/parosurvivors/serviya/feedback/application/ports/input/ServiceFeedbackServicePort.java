package com.parosurvivors.serviya.feedback.application.ports.input;

import com.parosurvivors.serviya.feedback.application.dto.command.SubmitServiceFeedbackCommand;
import com.parosurvivors.serviya.feedback.application.dto.result.ServiceFeedbackResult;
import com.parosurvivors.serviya.feedback.domain.ServiceFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de entrada de ServiceFeedbackService — fachada del feedback del cliente al servicio.
 * Recibe Command; las lecturas devuelven Result (vista agregada rating+resena). Nunca tipos web.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 5).
 */
public interface ServiceFeedbackServicePort {

    void submitServiceFeedback(SubmitServiceFeedbackCommand command);

    ServiceFeedbackResult getServiceFeedback(Long requestId);

    /**
     * Lee un feedback de servicio por su id propio (no por requestId). Devuelve {@link Optional#empty()}
     * si no existe — p.ej. si el feedback fue revertido (el revert borra la fila). Lo usa el detalle de
     * reporte de moderación, cuyo link almacena el feedbackId.
     */
    Optional<ServiceFeedbackResult> getServiceFeedbackById(Long feedbackId);

    Page<ServiceFeedbackResult> getServiceFeedbackList(Long serviceId, Pageable pageable);

    Page<ServiceFeedbackResult> getServiceFeedbackByClient(Long clientId, Pageable pageable);

    /**
     * Reseñas (feedback con comentario) más recientes de un servicio, hasta {@code limit}.
     * Devuelve el dominio {@link ServiceFeedback} (no Result) para que el detalle del servicio
     * pueda emparejar cada reseña con el perfil de su autor.
     */
    List<ServiceFeedback> getRecentServiceFeedback(Long serviceId, int limit);

    boolean revertFeedback(Long requestId);
}
