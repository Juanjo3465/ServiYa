package com.parosurvivors.serviya.feedback.application.ports.input;

import com.parosurvivors.serviya.feedback.domain.ServiceReview;

import java.util.List;

/**
 * Puerto de entrada de ServiceReviewService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 5).
 */
public interface ServiceReviewServicePort {

    ServiceReview createReview(int clientId, int requestId, String comment, List<Integer> tagIds);

    void deleteReview(int requestId, int clientId);
}
