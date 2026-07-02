package com.parosurvivors.serviya.feedback.application.dto.query;

/**
 * Filtros para la búsqueda admin de feedback (RF-048).
 */
public record SearchAdminFeedbackQuery(
        String type,
        Long clientId,
        Long offererId,
        Long serviceId) {
}
