package com.parosurvivors.serviya.admin.infrastructure.dto.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Entrada web (Form) para asignar un rol a un usuario (admin). POST /api/v1/admin/users/{id}/roles (RF-065).
 * El userId va en el path. Cubre CLIENT/OFFERER; la promocion a ADMIN va por grantAdminRole.
 * TODO: revisar validaciones.
 */
@Schema(description = "Rol a asignar a un usuario")
public record AssignRoleForm(
        @NotNull Long roleId) {
}
