package com.parosurvivors.serviya.feedback.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reseña del oferente al cliente (con etiquetas). Mapea la tabla {@code client_reviews}.
 * {@code tagIds} corresponde a la tabla puente {@code client_review_tags}; no es una columna
 * de client_reviews, por eso el mapper de persistencia lo ignora.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientReview {
    private Long id;
    private Long requestId;
    private Long offererId;
    private String comment;
    private LocalDateTime createdAt;

    /** Ids del catálogo de etiquetas asociadas (persistidos en client_review_tags). */
    @Builder.Default
    private List<Long> tagIds = new ArrayList<>();

    // =====================================================
    // BUSINESS METHODS
    // =====================================================

    public boolean hasTags() {
        return tagIds != null && !tagIds.isEmpty();
    }
}
