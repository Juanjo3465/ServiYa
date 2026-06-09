package com.parosurvivors.serviya.requests.infrastructure.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Salida web (Response) del detalle de una solicitud para el admin. Mapea desde AdminRequestDetailResult.
 * TODO: revisar campos de auditoria.
 */
@Schema(description = "Detalle de una solicitud de servicio (vista admin)")
public record AdminRequestDetailResponse(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Long id,
        Long serviceId,
        Long previousRequestId,
        Long clientId,
        Long offererId,
        Long addressId,
        LocalDateTime scheduledDate,
        String status,
        Long updatedBy,
        BigDecimal requestedPrice,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        LocalDateTime updatedStatusAt) {
}
