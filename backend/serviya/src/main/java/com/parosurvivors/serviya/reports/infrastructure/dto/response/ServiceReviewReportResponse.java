package com.parosurvivors.serviya.reports.infrastructure.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Salida web (Response) del enlace creado al reportar una resena de servicio.
 * POST /api/v1/reports/service-reviews. Mapea desde el dominio ServiceReviewReport.
 */
@Schema(description = "Reporte de resena de servicio creado")
public record ServiceReviewReportResponse(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Long id,
        Long reportId,
        Long reviewId) {
}
