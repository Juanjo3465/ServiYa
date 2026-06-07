package com.parosurvivors.serviya.feedback.application.ports.input;

import com.parosurvivors.serviya.feedback.application.dto.FeedbackParts;
import com.parosurvivors.serviya.feedback.application.dto.ReviewRequest;

/**
 * Puerto de entrada de FeedbackFlow — flujo común de feedback (creación y borrado),
 * side-agnostic, usado por las fachadas Service/Client FeedbackService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 5).
 */
public interface FeedbackFlowPort {

    void submit(FeedbackParts parts, int requestId, Integer rating, ReviewRequest review);

    void remove(FeedbackParts parts, int requestId);
}
