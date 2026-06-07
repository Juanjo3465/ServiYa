package com.parosurvivors.serviya.feedback.application.ports.input;

import com.parosurvivors.serviya.feedback.application.dto.ReviewRequest;
import com.parosurvivors.serviya.feedback.application.dto.ServiceFeedbackResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Puerto de entrada de ServiceFeedbackService — fachada del feedback del cliente al servicio.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 5).
 */
public interface ServiceFeedbackServicePort {

    void submitServiceFeedback(int clientId, int requestId, Integer rating, ReviewRequest review);

    ServiceFeedbackResponse getServiceFeedback(int requestId);

    Page<ServiceFeedbackResponse> getServiceFeedbackList(int serviceId, Pageable pageable);

    Page<ServiceFeedbackResponse> getServiceFeedbackByClient(int clientId, Pageable pageable);

    boolean revertFeedback(int requestId);
}
