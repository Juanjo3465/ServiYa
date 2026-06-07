package com.parosurvivors.serviya.notifications.application.ports.input;

import com.parosurvivors.serviya.notifications.domain.Notification;

import java.util.List;
import java.util.Map;

/**
 * Puerto de entrada de NotificationService — orquesta la entrega multicanal.
 * protectedData NO se persiste; el adaptador del canal arma el mensaje final.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 8).
 */
public interface NotificationServicePort {

    void notify(Long userId, String type, String title, String message, String entityType, Long entityId,
                List<Long> channelIds, Map<String, String> protectedData);

    Notification createNotification(Long userId, String type, String title, String message,
                                    String entityType, Long entityId);
}
