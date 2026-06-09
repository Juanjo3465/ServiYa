package com.parosurvivors.serviya.admin.infrastructure.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Salida web (Response) del detalle de un usuario para el panel admin. GET /api/v1/admin/users/{id} (RF-081).
 * Mapea desde UserAdminDetailResult.
 * TODO: revisar campos.
 */
@Schema(description = "Detalle de un usuario (vista admin)")
public record UserAdminDetailResponse(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Long id,
        String email,
        Boolean banned,
        LocalDateTime deletedAt,
        LocalDateTime createdAt,
        List<String> roles,
        Integer reportsReceived,
        Integer reportsSent,
        Integer totalRequests,
        Integer totalServices) {
}
