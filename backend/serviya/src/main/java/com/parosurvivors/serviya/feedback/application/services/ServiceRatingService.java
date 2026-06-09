package com.parosurvivors.serviya.feedback.application.services;

import com.parosurvivors.serviya.feedback.application.ports.input.ServiceRatingServicePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceRatingPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ServiceRating;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ServiceRatingServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ServiceRatingService implements ServiceRatingServicePort {

    private final ServiceRatingPersistencePort serviceRatingPersistencePort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ServiceRating addRating(Long clientId, Long requestId, int rating) {
        throw new UnsupportedOperationException("TODO: addRating — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void deleteRating(Long requestId, Long clientId) {
        throw new UnsupportedOperationException("TODO: deleteRating — placeholder, ver estructura-servicios.docx");
    }
}
