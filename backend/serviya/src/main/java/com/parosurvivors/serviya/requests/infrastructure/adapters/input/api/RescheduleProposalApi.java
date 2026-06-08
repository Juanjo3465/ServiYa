package com.parosurvivors.serviya.requests.infrastructure.adapters.input.api;

import com.parosurvivors.serviya.requests.application.dto.RescheduleProposalResponse;
import com.parosurvivors.serviya.requests.domain.RescheduleProposal;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * Documentacion OpenAPI/Swagger de propuestas de reprogramacion (modulo 4, seccion 11).
 * Ver estructura-endpoints.md.
 */
@Tag(name = "Propuestas de reprogramacion", description = "Propuestas del oferente al cliente para reprogramar")
@SecurityRequirement(name = "bearerAuth")
public interface RescheduleProposalApi {

    @Operation(summary = "Crear una propuesta de reprogramacion (oferente)", description = "RF-023.")
    @ApiResponse(responseCode = "201", description = "Propuesta creada (PENDING)")
    ResponseEntity<RescheduleProposal> createProposal(
            @Parameter(description = "Cuerpo con 'requestId', 'reason' y 'proposedDate' (ISO-8601)") Map<String, String> body);

    @Operation(summary = "Listar las propuestas recibidas (cliente)", description = "RF-034.")
    @ApiResponse(responseCode = "200", description = "Propuestas recibidas")
    ResponseEntity<List<RescheduleProposalResponse>> getProposalsReceived(
            @Parameter(description = "Estados separados por coma") List<String> statuses);

    @Operation(summary = "Listar las propuestas enviadas (oferente)")
    @ApiResponse(responseCode = "200", description = "Propuestas enviadas")
    ResponseEntity<List<RescheduleProposalResponse>> getProposalsSent(
            @Parameter(description = "Estados separados por coma") List<String> statuses);

    @Operation(summary = "Listar el historial de propuestas de una solicitud")
    @ApiResponse(responseCode = "200", description = "Propuestas de la solicitud")
    ResponseEntity<List<RescheduleProposalResponse>> getProposalsByRequest(
            @Parameter(description = "Id de la solicitud") Long id);

    @Operation(summary = "Aceptar una propuesta (cliente)",
            description = "Dispara la reprogramacion y crea la nueva solicitud. RF-035.")
    @ApiResponse(responseCode = "200", description = "Nueva solicitud enlazada creada")
    ResponseEntity<ServiceRequest> acceptProposal(
            @Parameter(description = "Id de la propuesta") Long id,
            @Parameter(description = "Cuerpo con 'confirmedDate' (ISO-8601)") Map<String, String> body);

    @Operation(summary = "Rechazar una propuesta (cliente)", description = "RF-036.")
    @ApiResponse(responseCode = "204", description = "Propuesta rechazada")
    ResponseEntity<Void> rejectProposal(@Parameter(description = "Id de la propuesta") Long id);

    @Operation(summary = "Cancelar la propia propuesta (oferente)")
    @ApiResponse(responseCode = "204", description = "Propuesta cancelada")
    ResponseEntity<Void> cancelProposal(@Parameter(description = "Id de la propuesta") Long id);
}
