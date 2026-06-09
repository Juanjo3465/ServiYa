package com.parosurvivors.serviya.admin.infrastructure.dto.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entrada web (Form) para crear un usuario desde el panel admin. POST /api/v1/admin/users.
 * El adminId se extrae del JWT. El rol va en el body (puede ser CLIENT/OFFERER; ADMIN va por grantAdminRole).
 * TODO: revisar validaciones y campos de perfil.
 */
@Schema(description = "Datos para crear un usuario (admin)")
public record CreateUserByAdminForm(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String fullName,
        @NotBlank String role,
        String documentType,
        String documentNumber,
        String phone) {
}
