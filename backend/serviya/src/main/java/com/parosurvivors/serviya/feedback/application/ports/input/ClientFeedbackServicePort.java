package com.parosurvivors.serviya.feedback.application.ports.input;

import com.parosurvivors.serviya.feedback.application.dto.command.SubmitClientFeedbackCommand;
import com.parosurvivors.serviya.feedback.application.dto.result.ClientFeedbackResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Puerto de entrada de ClientFeedbackService — fachada del feedback del oferente al cliente.
 * Recibe Command; las lecturas devuelven Result (vista agregada rating+resena). Nunca tipos web.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 5).
 */
public interface ClientFeedbackServicePort {

    void submitClientFeedback(SubmitClientFeedbackCommand command);

    ClientFeedbackResult getClientFeedback(Long requestId);

    /**
     * Lee un feedback de cliente por su id propio (no por requestId). Devuelve {@link Optional#empty()}
     * si no existe — p.ej. si el feedback fue revertido (el revert borra la fila). Lo usa el detalle de
     * reporte de moderación, cuyo link almacena el feedbackId.
     */
    Optional<ClientFeedbackResult> getClientFeedbackById(Long feedbackId);

    Page<ClientFeedbackResult> getClientFeedbackList(Long clientId, Pageable pageable);

    Page<ClientFeedbackResult> getClientFeedbackByOfferer(Long offererId, Pageable pageable);

    boolean revertFeedback(Long requestId);
}
