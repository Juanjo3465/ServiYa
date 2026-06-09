package com.parosurvivors.serviya.admin.infrastructure.dto.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entrada web (Form) para eliminar feedback inapropiado no reportado (admin). POST /api/v1/admin/reviews/remove
 * (RF-049). Crea internamente un reporte (admin como reportante) y lo resuelve via revertFeedbackFromReport.
 * El adminId se extrae del JWT.
 * TODO: revisar campos exactos (que identifican la resena a eliminar) y validaciones.
 */
@Schema(description = "Datos para eliminar directamente una resena inapropiada")
public record RemoveFeedbackForm(
        @NotBlank String targetType,
        @NotNull Long targetId,
        @NotNull Long reportedUserId,
        @NotBlank String category,
        String reason) {
}
