package com.parosurvivors.serviya.notifications.infrastructure.adapters.input.api;

import com.parosurvivors.serviya.notifications.application.dto.NotificationDeliveryResponse;
import com.parosurvivors.serviya.notifications.domain.NotificationChannel;
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
 * Documentacion OpenAPI/Swagger de las notificaciones del usuario (modulo 8, seccion 18).
 * notify/deliver son internos y no se exponen. Ver estructura-endpoints.md.
 */
@Tag(name = "Notificaciones", description = "Bandeja de notificaciones del usuario y canales disponibles")
@SecurityRequirement(name = "bearerAuth")
public interface NotificationApi {

    @Operation(summary = "Listar las notificaciones propias con filtros",
            description = "Filtros: read, channelId, status. RF-061, RF-062, RF-082..RF-089.")
    @ApiResponse(responseCode = "200", description = "Pagina de notificaciones")
    ResponseEntity<Page<NotificationDeliveryResponse>> getDeliveries(
            @Parameter(description = "Leida/no leida (opcional)") Boolean read,
            @Parameter(description = "Id del canal (opcional)") Long channelId,
            @Parameter(description = "Estado del envio: PENDING/SENT/FAILED (opcional)") String status,
            Pageable pageable);

    @Operation(summary = "Marcar una notificacion como leida")
    @ApiResponse(responseCode = "204", description = "Notificacion marcada como leida")
    ResponseEntity<Void> markAsRead(@Parameter(description = "Id del delivery") Long id);

    @Operation(summary = "Listar los canales de notificacion disponibles")
    @ApiResponse(responseCode = "200", description = "Canales disponibles")
    ResponseEntity<List<NotificationChannel>> getChannels();
}
