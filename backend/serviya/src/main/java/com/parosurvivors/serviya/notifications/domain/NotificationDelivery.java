package com.parosurvivors.serviya.notifications.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entrega de una notificación por un canal (estado de envío + lectura).
 * Mapea la tabla {@code notification_deliveries}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDelivery {
    private Long id;
    private Long notificationId;
    private Integer channelId;
    private DeliveryStatus deliveryStatus;
    private LocalDateTime readAt;
    private LocalDateTime sentAt;

    // =====================================================
    // BUSINESS METHODS
    // =====================================================

    public boolean isRead() {
        return deliveryStatus == DeliveryStatus.READ || readAt != null;
    }

    public void markAsSent() {
        this.deliveryStatus = DeliveryStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.deliveryStatus = DeliveryStatus.FAILED;
    }

    public void markAsRead() {
        if (isRead()) {
            return;
        }
        this.deliveryStatus = DeliveryStatus.READ;
        this.readAt = LocalDateTime.now();
    }
}
