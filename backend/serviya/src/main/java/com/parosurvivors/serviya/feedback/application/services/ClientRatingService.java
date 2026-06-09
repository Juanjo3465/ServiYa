package com.parosurvivors.serviya.feedback.application.services;

import com.parosurvivors.serviya.feedback.application.ports.input.ClientRatingServicePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientRatingPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ClientRating;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ClientRatingServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ClientRatingService implements ClientRatingServicePort {

    private final ClientRatingPersistencePort clientRatingPersistencePort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ClientRating addRating(Long offererId, Long requestId, Long clientId, int rating) {
        throw new UnsupportedOperationException("TODO: addRating — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void deleteRating(Long requestId, Long offererId) {
        throw new UnsupportedOperationException("TODO: deleteRating — placeholder, ver estructura-servicios.docx");
    }
}
