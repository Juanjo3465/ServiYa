package com.parosurvivors.serviya.requests.infrastructure.adapters.input.api;

import com.parosurvivors.serviya.requests.application.dto.RequestHistoryResponse;
import com.parosurvivors.serviya.requests.application.dto.ServiceRequestDetailResponse;
import com.parosurvivors.serviya.requests.application.dto.ServiceRequestResponse;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * Documentacion OpenAPI/Swagger de solicitudes de servicio: consultas y transiciones de estado
 * (modulo 4, seccion 10). Ver estructura-endpoints.md.
 */
@Tag(name = "Solicitudes de servicio", description = "Consultas y maquina de estados de las solicitudes")
@SecurityRequirement(name = "bearerAuth")
public interface ServiceRequestApi {

    @Operation(summary = "Listar las solicitudes propias como cliente",
            description = "Filtrables por estados (coma). RF-030, RF-032, RF-038.")
    @ApiResponse(responseCode = "200", description = "Pagina de solicitudes")
    ResponseEntity<Page<ServiceRequestResponse>> getClientRequests(
            @Parameter(description = "Estados separados por coma, ej. PENDING,ACCEPTED") List<String> statuses,
            Pageable pageable);

    @Operation(summary = "Listar las solicitudes propias como oferente",
            description = "Filtrables por estados. RF-016, RF-020, RF-039.")
    @ApiResponse(responseCode = "200", description = "Pagina de solicitudes")
    ResponseEntity<Page<ServiceRequestResponse>> getOffererRequests(
            @Parameter(description = "Estados separados por coma") List<String> statuses, Pageable pageable);

    @Operation(summary = "Detalle de una solicitud para una de las partes", description = "RF-030.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle de la solicitud"),
            @ApiResponse(responseCode = "403", description = "El solicitante no es parte de la solicitud")
    })
    ResponseEntity<ServiceRequestDetailResponse> getRequestDetail(
            @Parameter(description = "Id de la solicitud") Long id);

    @Operation(summary = "Historial de reprogramaciones de una solicitud",
            description = "Recorre la cadena previousRequestId.")
    @ApiResponse(responseCode = "200", description = "Cadena de solicitudes enlazadas")
    ResponseEntity<List<RequestHistoryResponse>> getRequestHistory(
            @Parameter(description = "Id de la solicitud") Long id);

    @Operation(summary = "Crear una solicitud de servicio",
            description = "Valida servicio activo, disponibilidad y radio. RF-029.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitud creada"),
            @ApiResponse(responseCode = "422", description = "Validacion de negocio fallida (radio/disponibilidad)")
    })
    ResponseEntity<ServiceRequest> createRequest(
            @Parameter(description = "Cuerpo con 'serviceId', 'addressId' y 'scheduledDate' (ISO-8601)") Map<String, String> body);

    @Operation(summary = "Aceptar una solicitud (oferente)", description = "RF-017.")
    @ApiResponse(responseCode = "204", description = "Solicitud aceptada")
    ResponseEntity<Void> acceptRequest(@Parameter(description = "Id de la solicitud") Long id);

    @Operation(summary = "Rechazar una solicitud (oferente)", description = "RF-018.")
    @ApiResponse(responseCode = "204", description = "Solicitud rechazada")
    ResponseEntity<Void> rejectRequest(@Parameter(description = "Id de la solicitud") Long id);

    @Operation(summary = "Cancelar una solicitud", description = "RF-022, RF-031.")
    @ApiResponse(responseCode = "204", description = "Solicitud cancelada")
    ResponseEntity<Void> cancelRequest(@Parameter(description = "Id de la solicitud") Long id);

    @Operation(summary = "Marcar como presuntamente cumplida (oferente)", description = "RF-019.")
    @ApiResponse(responseCode = "204", description = "Solicitud marcada como presuntamente cumplida")
    ResponseEntity<Void> markCompleted(@Parameter(description = "Id de la solicitud") Long id);

    @Operation(summary = "Confirmar la realizacion del servicio (cliente)", description = "RF-037.")
    @ApiResponse(responseCode = "204", description = "Servicio confirmado como completado")
    ResponseEntity<Void> confirmCompletion(@Parameter(description = "Id de la solicitud") Long id);

    @Operation(summary = "Marcar como no prestada", description = "Borra en cascada el feedback. RF-073.")
    @ApiResponse(responseCode = "204", description = "Solicitud marcada como no prestada")
    ResponseEntity<Void> markNotProvided(@Parameter(description = "Id de la solicitud") Long id);

    @Operation(summary = "Reprogramar una solicitud (cliente)",
            description = "Crea una nueva solicitud enlazada via previousRequestId. RF-033.")
    @ApiResponse(responseCode = "201", description = "Nueva solicitud enlazada creada")
    ResponseEntity<ServiceRequest> rescheduleRequest(
            @Parameter(description = "Id de la solicitud") Long id,
            @Parameter(description = "Cuerpo con 'newDate' (ISO-8601)") Map<String, String> body);
}
