package com.parosurvivors.serviya.feedback.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Calificación (1-5) del oferente al cliente. Mapea la tabla {@code client_ratings}
 * (una por solicitud).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientRating {
    private Long id;
    private Long requestId;
    private Long offererId;
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
