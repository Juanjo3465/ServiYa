package com.parosurvivors.serviya.feedback.application.services;

import com.parosurvivors.serviya.feedback.application.dto.ClientFeedbackResponse;
import com.parosurvivors.serviya.feedback.application.dto.ReviewRequest;
import com.parosurvivors.serviya.feedback.application.ports.input.ClientFeedbackServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.ClientRatingServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.ClientReviewServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.FeedbackFlowPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ClientFeedbackServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ClientFeedbackService implements ClientFeedbackServicePort {

    private final FeedbackFlowPort feedbackFlowPort;
    private final ClientRatingServicePort clientRatingServicePort;
    private final ClientReviewServicePort clientReviewServicePort;

    @Override
    public void submitClientFeedback(Long offererId, Long requestId, Long clientId, Integer rating, ReviewRequest review) {
        throw new UnsupportedOperationException("TODO: submitClientFeedback — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public ClientFeedbackResponse getClientFeedback(Long requestId) {
        throw new UnsupportedOperationException("TODO: getClientFeedback — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Page<ClientFeedbackResponse> getClientFeedbackList(Long clientId, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: getClientFeedbackList — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Page<ClientFeedbackResponse> getClientFeedbackByOfferer(Long offererId, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: getClientFeedbackByOfferer — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public boolean revertFeedback(Long requestId) {
        throw new UnsupportedOperationException("TODO: revertFeedback — placeholder, ver estructura-servicios.docx");
    }
}
