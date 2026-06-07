package com.parosurvivors.serviya.feedback.application.ports.input;

import com.parosurvivors.serviya.feedback.domain.ServiceReview;

import java.util.List;

/**
 * Puerto de entrada de ServiceReviewService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 5).
 */
public interface ServiceReviewServicePort {

    ServiceReview createReview(Long clientId, Long requestId, String comment, List<Long> tagIds);

    void deleteReview(Long requestId, Long clientId);
}
