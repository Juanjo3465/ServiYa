package com.parosurvivors.serviya.feedback.application.services;

import com.parosurvivors.serviya.feedback.application.dto.ReviewRequest;
import com.parosurvivors.serviya.feedback.application.dto.ServiceFeedbackResponse;
import com.parosurvivors.serviya.feedback.application.ports.input.FeedbackFlowPort;
import com.parosurvivors.serviya.feedback.application.ports.input.ServiceFeedbackServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.ServiceRatingServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.ServiceReviewServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ServiceFeedbackServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ServiceFeedbackService implements ServiceFeedbackServicePort {

    private final FeedbackFlowPort feedbackFlowPort;
    private final ServiceRatingServicePort serviceRatingServicePort;
    private final ServiceReviewServicePort serviceReviewServicePort;

    @Override
    public void submitServiceFeedback(Long clientId, Long requestId, Integer rating, ReviewRequest review) {
        throw new UnsupportedOperationException("TODO: submitServiceFeedback — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public ServiceFeedbackResponse getServiceFeedback(Long requestId) {
        throw new UnsupportedOperationException("TODO: getServiceFeedback — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Page<ServiceFeedbackResponse> getServiceFeedbackList(Long serviceId, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: getServiceFeedbackList — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Page<ServiceFeedbackResponse> getServiceFeedbackByClient(Long clientId, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: getServiceFeedbackByClient — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public boolean revertFeedback(Long requestId) {
        throw new UnsupportedOperationException("TODO: revertFeedback — placeholder, ver estructura-servicios.docx");
    }
}
