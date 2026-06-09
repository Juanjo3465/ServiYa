package com.parosurvivors.serviya.notifications.infrastructure.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Salida web (Response) de una notificacion entregada al usuario. GET /api/v1/notifications.
 * Mapea desde NotificationDeliveryResult.
 * TODO: revisar campos.
 */
@Schema(description = "Notificacion entregada (con estado y lectura)")
public record NotificationDeliveryResponse(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Long deliveryId,
        Long notificationId,
        Integer channelId,
        String deliveryStatus,
        LocalDateTime readAt,
        LocalDateTime sentAt,
        String notificationType,
        String title,
        String message,
        String entityType,
        Long entityId,
        LocalDateTime createdAt) {
}
