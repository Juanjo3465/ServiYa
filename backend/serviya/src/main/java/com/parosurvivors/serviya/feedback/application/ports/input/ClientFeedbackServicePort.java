package com.parosurvivors.serviya.feedback.application.ports.input;

import com.parosurvivors.serviya.feedback.application.dto.command.SubmitClientFeedbackCommand;
import com.parosurvivors.serviya.feedback.application.dto.result.ClientFeedbackResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Puerto de entrada de ClientFeedbackService — fachada del feedback del oferente al cliente.
 * Recibe Command; las lecturas devuelven Result (vista agregada rating+resena). Nunca tipos web.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 5).
 */
public interface ClientFeedbackServicePort {

    void submitClientFeedback(SubmitClientFeedbackCommand command);

    ClientFeedbackResult getClientFeedback(Long requestId);

    Page<ClientFeedbackResult> getClientFeedbackList(Long clientId, Pageable pageable);

    Page<ClientFeedbackResult> getClientFeedbackByOfferer(Long offererId, Pageable pageable);

    boolean revertFeedback(Long requestId);
}
