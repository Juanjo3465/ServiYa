package com.parosurvivors.serviya.admin.application.dto.result;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Salida de aplicacion (Result) del detalle de un usuario para el panel admin (CQRS-light). Vista agregada:
 * datos basicos + roles + conteos (reportes, solicitudes, servicios). No pasa por una unica entidad de dominio.
 * Lo devuelve AdminService.getUserAdminDetail.
 * TODO: revisar campos enriquecidos (metricas, conteos).
 */
public record UserAdminDetailResult(
        Long id,
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
