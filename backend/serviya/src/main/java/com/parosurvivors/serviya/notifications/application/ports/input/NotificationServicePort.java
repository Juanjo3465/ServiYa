package com.parosurvivors.serviya.notifications.application.ports.input;

import com.parosurvivors.serviya.notifications.domain.ChannelName;
import com.parosurvivors.serviya.notifications.domain.Notification;

import java.util.Map;
import java.util.Set;

/**
 * Puerto de entrada de NotificationService — orquesta la entrega multicanal.
 * protectedData NO se persiste; el adaptador del canal arma el mensaje final.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 8).
 */
public interface NotificationServicePort {

    /**
     * Registra la notificación y la entrega por los {@code channels} indicados (por nombre, no por id).
     * Si {@code channels} es {@code null} o vacío se usa el canal por defecto INTERNAL. La resolución
     * nombre→id vive en la implementación (única fuente de verdad); los llamadores nunca manejan ids.
     */
    void notify(Long userId, String type, String title, String message, String entityType, Long entityId,
                Set<ChannelName> channels, Map<String, String> protectedData);

    Notification createNotification(Long userId, String type, String title, String message,
                                    String entityType, Long entityId);
}
