package com.parosurvivors.serviya.feedback.application.ports.input;

import com.parosurvivors.serviya.feedback.domain.ClientRating;

/**
 * Puerto de entrada de ClientRatingService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 5).
 */
public interface ClientRatingServicePort {

    ClientRating addRating(Long offererId, Long requestId, Long clientId, int rating);

    void deleteRating(Long requestId, Long offererId);
}
