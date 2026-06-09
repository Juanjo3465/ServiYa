package com.parosurvivors.serviya.feedback.infrastructure.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Salida web (Response) de una etiqueta del catalogo de resenas de cliente. GET /api/v1/client-review-tags.
 * Mapea desde el dominio ClientReviewTagCatalog.
 */
@Schema(description = "Etiqueta del catalogo de resenas de cliente")
public record ClientReviewTagResponse(
        Long id,
        String tagName,
        @Schema(description = "Sentimiento: P (positivo) o N (negativo)") String sentiment) {
}
