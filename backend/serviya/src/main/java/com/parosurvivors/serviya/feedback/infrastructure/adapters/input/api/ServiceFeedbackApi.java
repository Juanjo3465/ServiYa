package com.parosurvivors.serviya.feedback.infrastructure.adapters.input.api;

import com.parosurvivors.serviya.feedback.application.dto.ReviewRequest;
import com.parosurvivors.serviya.feedback.application.dto.ServiceFeedbackResponse;
import com.parosurvivors.serviya.feedback.domain.ServiceReviewTagCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Documentacion OpenAPI/Swagger del feedback del cliente al servicio (modulo 5, seccion 12).
 * Ver estructura-endpoints.md.
 */
@Tag(name = "Feedback de servicio", description = "Calificaciones y resenas que el cliente deja a un servicio")
public interface ServiceFeedbackApi {

    @Operation(summary = "Enviar feedback de servicio (rating y/o resena)",
            description = "Un solo POST agrupa rating + review (cualquiera puede venir null). RF-041, RF-045.")
    @ApiResponse(responseCode = "204", description = "Feedback registrado")
    ResponseEntity<Void> submitServiceFeedback(
            @Parameter(description = "Id de la solicitud") Long id,
            @Parameter(description = "Calificacion 1-5 (opcional)") Integer rating,
            ReviewRequest review);

    @Operation(summary = "Obtener el feedback de una solicitud", description = "Rating + resena emparejados.")
    @ApiResponse(responseCode = "200", description = "Feedback de la solicitud")
    ResponseEntity<ServiceFeedbackResponse> getServiceFeedback(
            @Parameter(description = "Id de la solicitud") Long id);

    @Operation(summary = "Listar el feedback recibido por un servicio", description = "RF-040, RF-046.")
    @ApiResponse(responseCode = "200", description = "Pagina de feedback")
    ResponseEntity<Page<ServiceFeedbackResponse>> getServiceFeedbackList(
            @Parameter(description = "Id del servicio") Long id, Pageable pageable);

    @Operation(summary = "Listar el feedback de servicio dejado por un cliente")
    @ApiResponse(responseCode = "200", description = "Pagina de feedback del cliente")
    ResponseEntity<Page<ServiceFeedbackResponse>> getServiceFeedbackByClient(
            @Parameter(description = "Id del cliente") Long id, Pageable pageable);

    @Operation(summary = "Obtener el catalogo de tags de resenas de servicio")
    @ApiResponse(responseCode = "200", description = "Catalogo de tags")
    ResponseEntity<List<ServiceReviewTagCatalog>> getCatalog();
}
