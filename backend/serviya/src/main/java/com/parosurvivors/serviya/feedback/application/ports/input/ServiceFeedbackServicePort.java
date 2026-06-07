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

    void submitServiceFeedback(Long clientId, Long requestId, Integer rating, ReviewRequest review);

    ServiceFeedbackResponse getServiceFeedback(Long requestId);

    Page<ServiceFeedbackResponse> getServiceFeedbackList(Long serviceId, Pageable pageable);

    Page<ServiceFeedbackResponse> getServiceFeedbackByClient(Long clientId, Pageable pageable);

    boolean revertFeedback(Long requestId);
}
