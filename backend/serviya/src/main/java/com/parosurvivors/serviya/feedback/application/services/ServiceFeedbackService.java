package com.parosurvivors.serviya.feedback.application.services;

import com.parosurvivors.serviya.feedback.application.dto.command.SubmitServiceFeedbackCommand;
import com.parosurvivors.serviya.feedback.application.dto.result.ServiceFeedbackResult;
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
    public void submitServiceFeedback(SubmitServiceFeedbackCommand command) {
        throw new UnsupportedOperationException("TODO: submitServiceFeedback — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public ServiceFeedbackResult getServiceFeedback(Long requestId) {
        throw new UnsupportedOperationException("TODO: getServiceFeedback — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Page<ServiceFeedbackResult> getServiceFeedbackList(Long serviceId, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: getServiceFeedbackList — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Page<ServiceFeedbackResult> getServiceFeedbackByClient(Long clientId, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: getServiceFeedbackByClient — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public boolean revertFeedback(Long requestId) {
        throw new UnsupportedOperationException("TODO: revertFeedback — placeholder, ver estructura-servicios.docx");
    }
}
