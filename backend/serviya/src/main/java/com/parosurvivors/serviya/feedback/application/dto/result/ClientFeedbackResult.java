package com.parosurvivors.serviya.feedback.application.dto.result;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Salida de aplicacion (Result) del feedback de cliente: rating + resena emparejados (CQRS-light).
 * Vista agregada que combina ClientRating + ClientReview (+ tags); no pasa por una unica entidad.
 * TODO: revisar campos.
 */
public record ClientFeedbackResult(
        Long requestId,
        Long clientId,
        Long offererId,
        Integer rating,
        String comment,
        List<String> tags,
        LocalDateTime createdAt) {
}
