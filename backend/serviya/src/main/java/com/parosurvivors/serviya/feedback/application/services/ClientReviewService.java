package com.parosurvivors.serviya.feedback.application.services;

import com.parosurvivors.serviya.feedback.application.ports.input.ClientReviewServicePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientReviewPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientReviewTagPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ClientReview;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ClientReviewServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ClientReviewService implements ClientReviewServicePort {

    private final ClientReviewPersistencePort clientReviewPersistencePort;
    private final ClientReviewTagPersistencePort clientReviewTagPersistencePort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ClientReview createReview(Long offererId, Long requestId, String comment, List<Long> tagIds) {
        throw new UnsupportedOperationException("TODO: createReview — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void deleteReview(Long requestId, Long offererId) {
        throw new UnsupportedOperationException("TODO: deleteReview — placeholder, ver estructura-servicios.docx");
    }
}
