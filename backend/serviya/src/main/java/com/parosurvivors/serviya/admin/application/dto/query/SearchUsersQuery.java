package com.parosurvivors.serviya.admin.application.dto.query;

/**
 * Entrada de aplicacion (Query) de la busqueda de usuarios del panel admin. GET /api/v1/admin/users (RF-068).
 * Se bindea desde query params. Campos no-nulos = filtros activos.
 * TODO: revisar campos de filtro.
 */
public record SearchUsersQuery(
        String email,
        String fullName,
        String role,
        Boolean banned,
        Boolean deleted) {
}
