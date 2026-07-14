package com.parosurvivors.serviya.notifications.application.services;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationChannelServicePort;
import com.parosurvivors.serviya.notifications.application.ports.output.NotificationChannelPersistencePort;
import com.parosurvivors.serviya.notifications.domain.NotificationChannel;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationChannelService implements NotificationChannelServicePort {

    private final NotificationChannelPersistencePort notificationChannelPersistencePort;

    @Override
    public List<NotificationChannel> getChannels() {
        return notificationChannelPersistencePort.findAll();
    }

    @Override
    public Optional<NotificationChannel> findByName(String name) {
        return notificationChannelPersistencePort.findByName(name);
    }
}
