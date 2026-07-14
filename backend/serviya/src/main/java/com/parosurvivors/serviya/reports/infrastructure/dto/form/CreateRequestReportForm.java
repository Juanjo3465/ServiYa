package com.parosurvivors.serviya.reports.infrastructure.dto.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entrada web (Form) para reportar una solicitud. POST /api/v1/reports/requests (RF-055, RF-057, RF-073).
 * El reporterId se extrae del JWT.
 * TODO: revisar validaciones.
 */
@Schema(description = "Datos para reportar una solicitud de servicio")
public record CreateRequestReportForm(
        @NotNull Long reportedUserId,
        @NotBlank String category,
        String customCategory,
        String reason,
        @NotNull Long requestId) {
}
