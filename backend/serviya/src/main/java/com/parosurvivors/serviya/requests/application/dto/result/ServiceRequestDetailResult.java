package com.parosurvivors.serviya.requests.application.dto.result;

import com.parosurvivors.serviya.requests.domain.RequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Salida de aplicacion (Result) del detalle de una solicitud para una de las partes (CQRS-light).
 * Vista agregada que NO pasa por una unica entidad de dominio (enriquece con datos del servicio y las
 * partes). Lo devuelve ServiceRequestQueryService.getRequestDetailForParty; el WebMapper lo traduce a Response.
 * TODO: completar campos enriquecidos (titulo del servicio, nombres de cliente/oferente, etc.).
 */
public record ServiceRequestDetailResult(
        Long id,
        Long serviceId,
        Long previousRequestId,
        Long clientId,
        Long offererId,
        Long addressId,
        LocalDateTime scheduledDate,
        RequestStatus status,
        BigDecimal requestedPrice,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        // TODO datos enriquecidos de la vista de detalle
        String serviceTitle,
        String clientName,
        String offererName) {
}
