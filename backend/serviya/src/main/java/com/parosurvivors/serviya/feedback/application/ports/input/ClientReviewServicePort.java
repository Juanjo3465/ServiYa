package com.parosurvivors.serviya.feedback.application.ports.input;

import com.parosurvivors.serviya.feedback.domain.ClientReview;

import java.util.List;

/**
 * Puerto de entrada de ClientReviewService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 5).
 */
public interface ClientReviewServicePort {

    ClientReview createReview(int offererId, int requestId, String comment, List<Integer> tagIds);

    void deleteReview(int requestId, int offererId);
}
