package com.parosurvivors.serviya.notifications.application.services;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationDeliveryServicePort;
import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.notifications.application.ports.output.NotificationPersistencePort;
import com.parosurvivors.serviya.notifications.domain.Notification;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de NotificationServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class NotificationService implements NotificationServicePort {

    private final NotificationPersistencePort notificationPersistencePort;
    private final NotificationDeliveryServicePort notificationDeliveryServicePort;

    @Override
    public void notify(Long userId, String type, String title, String message, String entityType, Long entityId, List<Long> channelIds, Map<String, String> protectedData) {
        throw new UnsupportedOperationException("TODO: notify — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Notification createNotification(Long userId, String type, String title, String message, String entityType, Long entityId) {
        throw new UnsupportedOperationException("TODO: createNotification — placeholder, ver estructura-servicios.docx");
    }
}
