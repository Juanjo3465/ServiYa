package com.parosurvivors.serviya.feedback.application.ports.input;

import com.parosurvivors.serviya.feedback.application.dto.ClientFeedbackResponse;
import com.parosurvivors.serviya.feedback.application.dto.ReviewRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Puerto de entrada de ClientFeedbackService — fachada del feedback del oferente al cliente.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 5).
 */
public interface ClientFeedbackServicePort {

    void submitClientFeedback(Long offererId, Long requestId, Long clientId, Integer rating, ReviewRequest review);

    ClientFeedbackResponse getClientFeedback(Long requestId);

    Page<ClientFeedbackResponse> getClientFeedbackList(Long clientId, Pageable pageable);

    Page<ClientFeedbackResponse> getClientFeedbackByOfferer(Long offererId, Pageable pageable);

    boolean revertFeedback(Long requestId);
}
