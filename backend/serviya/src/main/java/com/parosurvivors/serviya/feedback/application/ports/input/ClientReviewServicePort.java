package com.parosurvivors.serviya.feedback.application.ports.input;

import com.parosurvivors.serviya.feedback.domain.ClientReview;

import java.util.List;

/**
 * Puerto de entrada de ClientReviewService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 5).
 */
public interface ClientReviewServicePort {

    ClientReview createReview(Long offererId, Long requestId, String comment, List<Long> tagIds);

    void deleteReview(Long requestId, Long offererId);
}
