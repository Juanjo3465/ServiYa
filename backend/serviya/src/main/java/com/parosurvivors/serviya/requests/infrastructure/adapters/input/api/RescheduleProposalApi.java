package com.parosurvivors.serviya.requests.infrastructure.adapters.input.api;

import com.parosurvivors.serviya.requests.infrastructure.dto.form.AcceptProposalForm;
import com.parosurvivors.serviya.requests.infrastructure.dto.form.CreateRescheduleProposalForm;
import com.parosurvivors.serviya.requests.infrastructure.dto.response.RescheduleProposalResponse;
import com.parosurvivors.serviya.requests.infrastructure.dto.response.ServiceRequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Documentacion OpenAPI/Swagger de propuestas de reprogramacion (modulo 4, seccion 11).
 * Ver estructura-endpoints.md. Convencion: docs aqui; binding y @Parameter en el controller.
 */
@Tag(name = "Propuestas de reprogramacion", description = "Propuestas del oferente al cliente para reprogramar")
@SecurityRequirement(name = "bearerAuth")
public interface RescheduleProposalApi {

    @Operation(summary = "Crear una propuesta de reprogramacion (oferente)", description = "RF-023.")
    @ApiResponse(responseCode = "201", description = "Propuesta creada (PENDING)")
    ResponseEntity<RescheduleProposalResponse> createProposal(CreateRescheduleProposalForm form);

    @Operation(summary = "Listar las propuestas recibidas (cliente)", description = "RF-034.")
    @ApiResponse(responseCode = "200", description = "Propuestas recibidas")
    ResponseEntity<List<RescheduleProposalResponse>> getProposalsReceived(List<String> statuses);

    @Operation(summary = "Listar las propuestas enviadas (oferente)")
    @ApiResponse(responseCode = "200", description = "Propuestas enviadas")
    ResponseEntity<List<RescheduleProposalResponse>> getProposalsSent(List<String> statuses);

    @Operation(summary = "Listar el historial de propuestas de una solicitud")
    @ApiResponse(responseCode = "200", description = "Propuestas de la solicitud")
    ResponseEntity<List<RescheduleProposalResponse>> getProposalsByRequest(Long id);

    @Operation(summary = "Aceptar una propuesta (cliente)",
            description = "Dispara la reprogramacion y crea la nueva solicitud. RF-035.")
    @ApiResponse(responseCode = "200", description = "Nueva solicitud enlazada creada")
    ResponseEntity<ServiceRequestResponse> acceptProposal(Long id, AcceptProposalForm form);

    @Operation(summary = "Rechazar una propuesta (cliente)", description = "RF-036.")
    @ApiResponse(responseCode = "204", description = "Propuesta rechazada")
    ResponseEntity<Void> rejectProposal(Long id);

    @Operation(summary = "Cancelar la propia propuesta (oferente)")
    @ApiResponse(responseCode = "204", description = "Propuesta cancelada")
    ResponseEntity<Void> cancelProposal(Long id);
}
