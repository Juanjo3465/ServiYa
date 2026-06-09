package com.parosurvivors.serviya.feedback.application.dto.result;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Salida de aplicacion (Result) del feedback de servicio: rating + resena emparejados (CQRS-light).
 * Vista agregada que combina ServiceRating + ServiceReview (+ tags); no pasa por una unica entidad.
 * TODO: revisar campos.
 */
public record ServiceFeedbackResult(
        Long requestId,
        Long serviceId,
        Long clientId,
        Integer rating,
        String comment,
        List<String> tags,
        LocalDateTime createdAt) {
}
