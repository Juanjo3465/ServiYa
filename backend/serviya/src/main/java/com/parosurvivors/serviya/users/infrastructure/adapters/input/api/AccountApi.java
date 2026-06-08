package com.parosurvivors.serviya.users.infrastructure.adapters.input.api;

import com.parosurvivors.serviya.users.domain.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * Documentacion OpenAPI/Swagger del controlador de gestion de cuenta del usuario autenticado (modulo 2).
 * El id del usuario se extrae del JWT (/me). Ver estructura-endpoints.md (seccion 2).
 */
@Tag(name = "Cuenta", description = "Gestion de la cuenta del usuario autenticado (/me)")
@SecurityRequirement(name = "bearerAuth")
public interface AccountApi {

    @Operation(summary = "Cambiar la contrasena propia", description = "RF-007.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contrasena actualizada"),
            @ApiResponse(responseCode = "400", description = "Contrasena actual incorrecta")
    })
    ResponseEntity<Void> changePassword(
            @Parameter(description = "Cuerpo con 'currentPassword' y 'newPassword'") Map<String, String> body);

    @Operation(summary = "Cambiar el correo propio", description = "RF-007.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Correo actualizado"),
            @ApiResponse(responseCode = "409", description = "El correo ya esta en uso")
    })
    ResponseEntity<Void> changeEmail(
            @Parameter(description = "Cuerpo con 'newEmail'") Map<String, String> body);

    @Operation(summary = "Eliminar la cuenta propia (soft delete)",
            description = "Orquesta la eliminacion: cancela solicitudes y desactiva servicios. RF-008.")
    @ApiResponse(responseCode = "204", description = "Cuenta eliminada")
    ResponseEntity<Void> deleteOwnAccount();

    @Operation(summary = "Adquirir el rol OFFERER (auto-servicio)", description = "RF-010.")
    @ApiResponse(responseCode = "204", description = "Rol asignado")
    ResponseEntity<Void> acquireOffererRole();

    @Operation(summary = "Adquirir el rol CLIENT (auto-servicio)", description = "RF-011.")
    @ApiResponse(responseCode = "204", description = "Rol asignado")
    ResponseEntity<Void> acquireClientRole();

    @Operation(summary = "Listar los roles propios")
    @ApiResponse(responseCode = "200", description = "Roles del usuario")
    ResponseEntity<List<Role>> getOwnRoles();
}
