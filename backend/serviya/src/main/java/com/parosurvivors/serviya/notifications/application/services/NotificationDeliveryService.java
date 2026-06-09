package com.parosurvivors.serviya.notifications.application.services;

import com.parosurvivors.serviya.notifications.application.dto.result.NotificationDeliveryResult;
import com.parosurvivors.serviya.notifications.application.ports.input.NotificationDeliveryServicePort;
import com.parosurvivors.serviya.notifications.application.ports.output.NotificationChannelPersistencePort;
import com.parosurvivors.serviya.notifications.application.ports.output.NotificationDeliveryPersistencePort;
import com.parosurvivors.serviya.notifications.domain.NotificationDelivery;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de NotificationDeliveryServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class NotificationDeliveryService implements NotificationDeliveryServicePort {

    private final NotificationDeliveryPersistencePort notificationDeliveryPersistencePort;
    private final NotificationChannelPersistencePort notificationChannelPersistencePort;

    @Override
    public NotificationDelivery deliver(Long notificationId, Long channelId, Map<String, String> protectedData) {
        throw new UnsupportedOperationException("TODO: deliver — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Page<NotificationDeliveryResult> getDeliveries(Long userId, Boolean read, Long channelId, String status, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: getDeliveries — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public String getDeliveryStatus(Long notificationId, Long channelId) {
        throw new UnsupportedOperationException("TODO: getDeliveryStatus — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public boolean isRead(Long notificationId, Long channelId) {
        throw new UnsupportedOperationException("TODO: isRead — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void markAsRead(Long deliveryId) {
        throw new UnsupportedOperationException("TODO: markAsRead — placeholder, ver estructura-servicios.docx");
    }
}
