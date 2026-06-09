package com.parosurvivors.serviya.admin.infrastructure.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Salida web (Response) de un usuario en el panel admin (fila de listado y resultado de creacion).
 * Mapea desde el dominio User. GET /api/v1/admin/users, POST /api/v1/admin/users.
 * TODO: revisar campos.
 */
@Schema(description = "Usuario (vista de listado admin)")
public record UserSummaryResponse(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Long id,
        String email,
        Boolean banned,
        LocalDateTime deletedAt,
        LocalDateTime createdAt) {
}
