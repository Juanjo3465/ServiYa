package com.parosurvivors.serviya.feedback.application.ports.input;

import com.parosurvivors.serviya.feedback.application.dto.command.SubmitServiceFeedbackCommand;
import com.parosurvivors.serviya.feedback.application.dto.result.ServiceFeedbackResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    boolean revertFeedback(Long requestId);
}
