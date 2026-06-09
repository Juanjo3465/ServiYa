package com.parosurvivors.serviya.reports.infrastructure.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Salida web (Response) del enlace creado al reportar una resena de cliente.
 * POST /api/v1/reports/client-reviews. Mapea desde el dominio ClientReviewReport.
 */
@Schema(description = "Reporte de resena de cliente creado")
public record ClientReviewReportResponse(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Long id,
        Long reportId,
        Long reviewId) {
}
