package com.parosurvivors.serviya.profiles.infrastructure.adapters.input.api;

import com.parosurvivors.serviya.profiles.application.dto.OffererProfilePublicResponse;
import com.parosurvivors.serviya.profiles.application.dto.OffererProfileSummaryResponse;
import com.parosurvivors.serviya.profiles.application.dto.PatchOffererProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/**
 * Documentacion OpenAPI/Swagger del perfil de oferente (modulo 2, seccion 5). Ver estructura-endpoints.md.
 */
@Tag(name = "Perfil de oferente", description = "Perfil publico del oferente y edicion del propio")
public interface OffererProfileApi {

    @Operation(summary = "Obtener el perfil publico de un oferente", description = "RF-015, RF-027.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil publico del oferente"),
            @ApiResponse(responseCode = "404", description = "Oferente no encontrado")
    })
    ResponseEntity<OffererProfilePublicResponse> getPublicProfile(
            @Parameter(description = "Id del usuario oferente") Long id);

    @Operation(summary = "Obtener el resumen del perfil de un oferente")
    @ApiResponse(responseCode = "200", description = "Resumen del perfil")
    ResponseEntity<OffererProfileSummaryResponse> getProfileSummary(
            @Parameter(description = "Id del usuario oferente") Long id);

    @Operation(summary = "Actualizar parcialmente el perfil de oferente propio (PATCH)",
            description = "Campos editables: whatsappNumber, publicDescription, specialty. RF-015, RF-012.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Perfil actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos")
    })
    ResponseEntity<Void> patchOffererProfile(PatchOffererProfileRequest dto);
}
