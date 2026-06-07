package com.parosurvivors.serviya.feedback.application.ports.input;

import com.parosurvivors.serviya.feedback.domain.ServiceRating;

/**
 * Puerto de entrada de ServiceRatingService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 5).
 */
public interface ServiceRatingServicePort {

    ServiceRating addRating(int clientId, int requestId, int rating);

    void deleteRating(int requestId, int clientId);
}
