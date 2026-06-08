package com.parosurvivors.serviya.feedback.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Calificación (1-5) del cliente al servicio. Mapea la tabla {@code service_ratings}
 * (una por solicitud).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRating {
    private Long id;
    private Long requestId;
    private Long clientId;
    private Integer rating;
    private LocalDateTime createdAt;

    // =====================================================
    // BUSINESS METHODS
    // =====================================================

    public boolean isValid() {
        return rating != null && rating >= 1 && rating <= 5;
    }
}
