package com.parosurvivors.serviya.feedback.infrastructure.adapters.input.api;

import com.parosurvivors.serviya.feedback.application.dto.ClientFeedbackResponse;
import com.parosurvivors.serviya.feedback.application.dto.ReviewRequest;
import com.parosurvivors.serviya.feedback.domain.ClientReviewTagCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Documentacion OpenAPI/Swagger del feedback del oferente al cliente (modulo 5, seccion 13).
 * Ver estructura-endpoints.md.
 */
@Tag(name = "Feedback de cliente", description = "Calificaciones y resenas que el oferente deja a un cliente")
public interface ClientFeedbackApi {

    @Operation(summary = "Enviar feedback de cliente (rating y/o resena)",
            description = "El oferente califica al cliente. RF-043, RF-044.")
    @ApiResponse(responseCode = "204", description = "Feedback registrado")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<Void> submitClientFeedback(
            @Parameter(description = "Id de la solicitud") Long id,
            @Parameter(description = "Id del cliente calificado") Long clientId,
            @Parameter(description = "Calificacion 1-5 (opcional)") Integer rating,
            ReviewRequest review);

    @Operation(summary = "Obtener el feedback de cliente de una solicitud")
    @ApiResponse(responseCode = "200", description = "Feedback de la solicitud")
    ResponseEntity<ClientFeedbackResponse> getClientFeedback(
            @Parameter(description = "Id de la solicitud") Long id);

    @Operation(summary = "Listar el feedback recibido por un cliente", description = "RF-047.")
    @ApiResponse(responseCode = "200", description = "Pagina de feedback del cliente")
    ResponseEntity<Page<ClientFeedbackResponse>> getClientFeedbackList(
            @Parameter(description = "Id del cliente") Long id, Pageable pageable);

    @Operation(summary = "Listar el feedback de cliente dejado por un oferente")
    @ApiResponse(responseCode = "200", description = "Pagina de feedback del oferente")
    ResponseEntity<Page<ClientFeedbackResponse>> getClientFeedbackByOfferer(
            @Parameter(description = "Id del oferente") Long id, Pageable pageable);

    @Operation(summary = "Obtener el catalogo de tags de resenas de cliente")
    @ApiResponse(responseCode = "200", description = "Catalogo de tags")
    ResponseEntity<List<ClientReviewTagCatalog>> getCatalog();
}
