package com.parosurvivors.serviya.requests.application.dto.result;

import com.parosurvivors.serviya.requests.domain.RequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Salida de aplicacion (Result) del detalle de una solicitud para el admin (CQRS-light).
 * Como ServiceRequestDetailResult pero con campos de auditoria/moderacion adicionales.
 * Lo devuelve ServiceRequestQueryService.getRequestDetailForAdmin.
 * TODO: completar campos de auditoria.
 */
public record AdminRequestDetailResult(
        Long id,
        Long serviceId,
        Long previousRequestId,
        Long clientId,
        Long offererId,
        Long addressId,
        LocalDateTime scheduledDate,
        RequestStatus status,
        Long updatedBy,
        BigDecimal requestedPrice,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        LocalDateTime updatedStatusAt) {
}
