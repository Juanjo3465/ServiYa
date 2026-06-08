package com.parosurvivors.serviya.admin.infrastructure.adapters.input.api;

import com.parosurvivors.serviya.admin.application.dto.CreateUserRequest;
import com.parosurvivors.serviya.admin.application.dto.UserAdminDetailResponse;
import com.parosurvivors.serviya.admin.application.dto.UserFilterRequest;
import com.parosurvivors.serviya.admin.application.dto.UserSummaryResponse;
import com.parosurvivors.serviya.users.domain.Role;
import com.parosurvivors.serviya.users.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * Documentacion OpenAPI/Swagger de la administracion de usuarios y roles (modulo 9, seccion 17).
 * Ver estructura-endpoints.md. Todas requieren rol ADMIN.
 */
@Tag(name = "Administracion", description = "Gestion de usuarios y roles por un admin")
@SecurityRequirement(name = "bearerAuth")
public interface AdminApi {

    @Operation(summary = "Buscar usuarios con filtros", description = "RF-068.")
    @ApiResponse(responseCode = "200", description = "Pagina de usuarios")
    ResponseEntity<Page<UserSummaryResponse>> searchUsers(UserFilterRequest filters, Pageable pageable);

    @Operation(summary = "Detalle de un usuario para el panel admin", description = "RF-081.")
    @ApiResponse(responseCode = "200", description = "Detalle del usuario")
    ResponseEntity<UserAdminDetailResponse> getUserAdminDetail(@Parameter(description = "Id del usuario") Long id);

    @Operation(summary = "Crear un usuario (cualquier rol, incluido ADMIN)")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    ResponseEntity<User> createUserByAdmin(
            @Parameter(description = "Rol del nuevo usuario") String roleName, CreateUserRequest dto);

    @Operation(summary = "Banear a un usuario", description = "RF-069, RF-063.")
    @ApiResponse(responseCode = "204", description = "Usuario baneado")
    ResponseEntity<Void> banUser(@Parameter(description = "Id del usuario") Long id);

    @Operation(summary = "Desbanear a un usuario", description = "RF-070, RF-075.")
    @ApiResponse(responseCode = "204", description = "Usuario desbaneado")
    ResponseEntity<Void> unbanUser(@Parameter(description = "Id del usuario") Long id);

    @Operation(summary = "Eliminar a un usuario", description = "RF-068.")
    @ApiResponse(responseCode = "204", description = "Usuario eliminado")
    ResponseEntity<Void> deleteUser(@Parameter(description = "Id del usuario") Long id);

    @Operation(summary = "Listar todos los roles del sistema")
    @ApiResponse(responseCode = "200", description = "Roles disponibles")
    ResponseEntity<List<Role>> getRoles();

    @Operation(summary = "Listar los roles de un usuario", description = "RF-067.")
    @ApiResponse(responseCode = "200", description = "Roles del usuario")
    ResponseEntity<List<Role>> getUserRoles(@Parameter(description = "Id del usuario") Long id);

    @Operation(summary = "Asignar un rol (CLIENT/OFFERER) a un usuario", description = "RF-065.")
    @ApiResponse(responseCode = "204", description = "Rol asignado")
    ResponseEntity<Void> assignRole(@Parameter(description = "Id del usuario") Long id,
                                    @Parameter(description = "Cuerpo con 'roleId'") Map<String, Long> body);

    @Operation(summary = "Promover a un usuario existente a ADMIN")
    @ApiResponse(responseCode = "204", description = "Rol ADMIN concedido")
    ResponseEntity<Void> grantAdminRole(@Parameter(description = "Id del usuario") Long id);

    @Operation(summary = "Quitar un rol a un usuario", description = "RF-066.")
    @ApiResponse(responseCode = "204", description = "Rol removido")
    ResponseEntity<Void> removeRole(@Parameter(description = "Id del usuario") Long id,
                                    @Parameter(description = "Id del rol") Long roleId);
}
