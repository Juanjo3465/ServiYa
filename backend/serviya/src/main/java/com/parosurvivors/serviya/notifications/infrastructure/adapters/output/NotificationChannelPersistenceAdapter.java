package com.parosurvivors.serviya.notifications.infrastructure.adapters.output;

import com.parosurvivors.serviya.notifications.application.ports.output.NotificationChannelPersistencePort;
import com.parosurvivors.serviya.notifications.domain.NotificationChannel;
import com.parosurvivors.serviya.notifications.infrastructure.mappers.NotificationChannelPersistenceMapper;
import com.parosurvivors.serviya.notifications.infrastructure.repositories.NotificationChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationChannelPersistenceAdapter implements NotificationChannelPersistencePort {

    private final NotificationChannelRepository repository;
    private final NotificationChannelPersistenceMapper mapper;

    @Override
    public List<NotificationChannel> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<NotificationChannel> findById(Integer id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<NotificationChannel> findByName(String name) {
        return repository.findByName(name).map(mapper::toDomain);
    }
}
