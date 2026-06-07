package com.parosurvivors.serviya.notifications.application.ports.input;

import com.parosurvivors.serviya.notifications.domain.NotificationChannel;

import java.util.List;

/**
 * Puerto de entrada de NotificationChannelService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 8).
 */
public interface NotificationChannelServicePort {

    List<NotificationChannel> getChannels();
}
