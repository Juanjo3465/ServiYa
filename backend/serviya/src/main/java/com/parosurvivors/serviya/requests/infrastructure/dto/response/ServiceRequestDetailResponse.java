package com.parosurvivors.serviya.requests.infrastructure.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Salida web (Response) del detalle de una solicitud para una parte. Mapea desde ServiceRequestDetailResult.
 * TODO: revisar campos enriquecidos.
 */
@Schema(description = "Detalle de una solicitud de servicio")
public record ServiceRequestDetailResponse(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Long id,
        Long serviceId,
        Long previousRequestId,
        Long clientId,
        Long offererId,
        Long addressId,
        LocalDateTime scheduledDate,
        String status,
        BigDecimal requestedPrice,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        String serviceTitle,
        String clientName,
        String offererName) {
}
