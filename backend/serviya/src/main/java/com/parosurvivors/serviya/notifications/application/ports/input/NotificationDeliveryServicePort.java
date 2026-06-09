package com.parosurvivors.serviya.notifications.application.ports.input;

import com.parosurvivors.serviya.notifications.application.dto.result.NotificationDeliveryResult;
import com.parosurvivors.serviya.notifications.domain.NotificationDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * Puerto de entrada de NotificationDeliveryService — entrega por canal y consulta de notificaciones.
 * deliver es interno; getDeliveries devuelve el Result agregado (delivery + notificacion). Nunca tipos web.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 8).
 */
public interface NotificationDeliveryServicePort {

    NotificationDelivery deliver(Long notificationId, Long channelId, Map<String, String> protectedData);

    Page<NotificationDeliveryResult> getDeliveries(Long userId, Boolean read, Long channelId,
                                                   String status, Pageable pageable);

    String getDeliveryStatus(Long notificationId, Long channelId);

    boolean isRead(Long notificationId, Long channelId);

    void markAsRead(Long deliveryId);
}
