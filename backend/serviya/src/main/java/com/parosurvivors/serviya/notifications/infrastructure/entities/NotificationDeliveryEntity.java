package com.parosurvivors.serviya.notifications.infrastructure.entities;

import com.parosurvivors.serviya.notifications.domain.DeliveryStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_deliveries")
@Getter
@Setter
public class NotificationDeliveryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "channel_id", nullable = false)
    private Integer channelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    private DeliveryStatus deliveryStatus;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    // columnDefinition con DEFAULT 0 para que ddl-auto:update pueda añadir la columna a tablas ya pobladas.
    @Column(name = "attempts", nullable = false, columnDefinition = "INT UNSIGNED NOT NULL DEFAULT 0")
    private Integer attempts;
}
