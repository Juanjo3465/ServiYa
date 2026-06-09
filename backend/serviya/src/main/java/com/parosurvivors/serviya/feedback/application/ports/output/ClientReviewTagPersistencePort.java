package com.parosurvivors.serviya.feedback.application.ports.output;

import java.util.List;

/**
 * Puerto de salida para la tabla puente {@code client_review_tags}. Maneja los enlaces
 * reseña-etiqueta con identificadores planos; la reseña ({@code ClientReview}) y sus
 * {@code tagIds} se componen en el servicio de aplicación.
 */
public interface ClientReviewTagPersistencePort {
    void addTags(Long reviewId, List<Long> tagIds);
    List<Long> findTagIdsByReviewId(Long reviewId);
    void deleteByReviewId(Long reviewId);
}
