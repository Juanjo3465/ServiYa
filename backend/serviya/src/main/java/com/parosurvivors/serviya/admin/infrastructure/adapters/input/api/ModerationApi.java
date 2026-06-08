package com.parosurvivors.serviya.admin.infrastructure.adapters.input.api;

import com.parosurvivors.serviya.admin.application.dto.CreateReportRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/**
 * Documentacion OpenAPI/Swagger de las acciones de moderacion del admin (modulo 9, seccion 16/17).
 * Ver estructura-endpoints.md. Todas requieren rol ADMIN.
 */
@Tag(name = "Moderacion", description = "Acciones de un admin sobre reportes y contenido")
@SecurityRequirement(name = "bearerAuth")
public interface ModerationApi {

    @Operation(summary = "Advertir al usuario reportado", description = "RF-060, RF-071.")
    @ApiResponse(responseCode = "204", description = "Advertencia aplicada y reporte cerrado")
    ResponseEntity<Void> warnUser(@Parameter(description = "Id del reporte") Long id);

    @Operation(summary = "Banear al usuario reportado", description = "RF-069, RF-063.")
    @ApiResponse(responseCode = "204", description = "Usuario baneado y reporte cerrado")
    ResponseEntity<Void> banUserFromReport(@Parameter(description = "Id del reporte") Long id);

    @Operation(summary = "Revertir el feedback reportado", description = "RF-049.")
    @ApiResponse(responseCode = "204", description = "Feedback revertido y reporte cerrado")
    ResponseEntity<Void> revertFeedback(@Parameter(description = "Id del reporte") Long id);

    @Operation(summary = "Cerrar el reporte sin penalizacion", description = "RF-059.")
    @ApiResponse(responseCode = "204", description = "Reporte cerrado")
    ResponseEntity<Void> closeReport(@Parameter(description = "Id del reporte") Long id);

    @Operation(summary = "Marcar la solicitud del reporte como no prestada", description = "RF-074.")
    @ApiResponse(responseCode = "204", description = "Solicitud marcada como no prestada y reporte cerrado")
    ResponseEntity<Void> markRequestAsNotProvided(@Parameter(description = "Id del reporte") Long id);

    @Operation(summary = "Eliminar feedback inapropiado no reportado",
            description = "Crea internamente un reporte y lo resuelve (auditoria). RF-049.")
    @ApiResponse(responseCode = "204", description = "Feedback eliminado con rastro de auditoria")
    ResponseEntity<Void> removeFeedbackDirectly(CreateReportRequest reportData);
}
