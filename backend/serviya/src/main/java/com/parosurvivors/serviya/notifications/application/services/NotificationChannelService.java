package com.parosurvivors.serviya.notifications.application.services;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationChannelServicePort;
import com.parosurvivors.serviya.notifications.application.ports.output.NotificationChannelPersistencePort;
import com.parosurvivors.serviya.notifications.domain.NotificationChannel;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de NotificationChannelServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class NotificationChannelService implements NotificationChannelServicePort {

    private final NotificationChannelPersistencePort notificationChannelPersistencePort;

    @Override
    public List<NotificationChannel> getChannels() {
        throw new UnsupportedOperationException("TODO: getChannels — placeholder, ver estructura-servicios.docx");
    }
}
