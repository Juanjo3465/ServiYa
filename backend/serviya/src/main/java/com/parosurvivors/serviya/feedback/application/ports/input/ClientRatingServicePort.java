package com.parosurvivors.serviya.feedback.application.ports.input;

import com.parosurvivors.serviya.feedback.domain.ClientRating;

/**
 * Puerto de entrada de ClientRatingService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 5).
 */
public interface ClientRatingServicePort {

    ClientRating addRating(int offererId, int requestId, int clientId, int rating);

    void deleteRating(int requestId, int offererId);
}
