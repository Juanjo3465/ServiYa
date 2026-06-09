package com.parosurvivors.serviya.users.infrastructure.dto.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entrada web (Form) de registro de un nuevo usuario. POST /api/v1/auth/register (RF-002, RF-004).
 * Agrupa credenciales + datos de perfil + rol deseado + consentimiento; el servicio delega la
 * creacion comun en UserCreationService.createUserAccount.
 * TODO: revisar campos exactos y validaciones contra documentacion-BD.docx.
 */
@Schema(description = "Datos de registro de un nuevo usuario (CLIENT u OFFERER)")
public record RegisterUserForm(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String fullName,
        // Rol deseado: CLIENT u OFFERER (nunca ADMIN). Va en el body, no en query.
        @NotBlank String role,
        // TODO datos de perfil (documentacion-BD.docx): tipo/numero de documento, telefono.
        String documentType,
        String documentNumber,
        String phone,
        @NotNull Boolean acceptedTerms) {
}
