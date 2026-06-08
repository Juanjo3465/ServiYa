package com.parosurvivors.serviya.notifications.infrastructure.adapters.output;

import com.parosurvivors.serviya.notifications.application.ports.output.NotificationPersistencePort;
import com.parosurvivors.serviya.notifications.domain.Notification;
import com.parosurvivors.serviya.notifications.infrastructure.entities.NotificationEntity;
import com.parosurvivors.serviya.notifications.infrastructure.mappers.NotificationPersistenceMapper;
import com.parosurvivors.serviya.notifications.infrastructure.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements NotificationPersistencePort {

    private final NotificationRepository repository;
    private final NotificationPersistenceMapper mapper;

    @Override
    public Notification save(Notification notification) {
        NotificationEntity saved = repository.save(mapper.toEntity(notification));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Notification> findByUserId(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
