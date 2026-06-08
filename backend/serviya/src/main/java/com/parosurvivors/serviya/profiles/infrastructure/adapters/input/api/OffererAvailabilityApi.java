package com.parosurvivors.serviya.profiles.infrastructure.adapters.input.api;

import com.parosurvivors.serviya.profiles.application.dto.SlotRequest;
import com.parosurvivors.serviya.profiles.domain.OffererAvailability;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Documentacion OpenAPI/Swagger de la disponibilidad general del oferente (modulo 2, seccion 9). RF-072.
 * Ver estructura-endpoints.md.
 */
@Tag(name = "Disponibilidad del oferente", description = "Plantilla general de horario del oferente autenticado")
@SecurityRequirement(name = "bearerAuth")
public interface OffererAvailabilityApi {

    @Operation(summary = "Obtener el horario general propio", description = "Ordenado por dia y hora.")
    @ApiResponse(responseCode = "200", description = "Horario del oferente")
    ResponseEntity<List<OffererAvailability>> getSchedule();

    @Operation(summary = "Reemplazar el horario general completo (PUT)",
            description = "Borra e inserta de forma atomica el calendario. RF-072.")
    @ApiResponse(responseCode = "204", description = "Horario guardado")
    ResponseEntity<Void> setSchedule(List<SlotRequest> slots);

    @Operation(summary = "Eliminar una franja horaria", description = "Verifica propiedad. RF-072.")
    @ApiResponse(responseCode = "204", description = "Franja eliminada")
    ResponseEntity<Void> deleteSlot(@Parameter(description = "Id de la franja") Long id);

    @Operation(summary = "Activar una franja horaria", description = "Verifica propiedad. RF-072.")
    @ApiResponse(responseCode = "204", description = "Franja activada")
    ResponseEntity<Void> activateSlot(@Parameter(description = "Id de la franja") Long id);

    @Operation(summary = "Desactivar una franja horaria", description = "Verifica propiedad. RF-072.")
    @ApiResponse(responseCode = "204", description = "Franja desactivada")
    ResponseEntity<Void> deactivateSlot(@Parameter(description = "Id de la franja") Long id);
}
