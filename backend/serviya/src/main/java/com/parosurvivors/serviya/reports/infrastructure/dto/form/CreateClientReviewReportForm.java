package com.parosurvivors.serviya.reports.infrastructure.dto.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entrada web (Form) para reportar una resena de cliente. POST /api/v1/reports/client-reviews (RF-056).
 * El reporterId se extrae del JWT.
 * TODO: revisar validaciones.
 */
@Schema(description = "Datos para reportar una resena de cliente")
public record CreateClientReviewReportForm(
        @NotNull Long reportedUserId,
        @NotBlank String category,
        String reason,
        @NotNull Long clientReviewId) {
}
