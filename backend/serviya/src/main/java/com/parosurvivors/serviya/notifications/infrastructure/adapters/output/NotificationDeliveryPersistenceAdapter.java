package com.parosurvivors.serviya.notifications.infrastructure.adapters.output;

import com.parosurvivors.serviya.notifications.application.ports.output.NotificationDeliveryPersistencePort;
import com.parosurvivors.serviya.notifications.domain.DeliveryStatus;
import com.parosurvivors.serviya.notifications.domain.NotificationDelivery;
import com.parosurvivors.serviya.notifications.infrastructure.entities.NotificationDeliveryEntity;
import com.parosurvivors.serviya.notifications.infrastructure.mappers.NotificationDeliveryPersistenceMapper;
import com.parosurvivors.serviya.notifications.infrastructure.repositories.NotificationDeliveryRepository;
import com.parosurvivors.serviya.notifications.infrastructure.repositories.NotificationDeliverySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationDeliveryPersistenceAdapter implements NotificationDeliveryPersistencePort {

    private final NotificationDeliveryRepository repository;
    private final NotificationDeliveryPersistenceMapper mapper;

    @Override
    public NotificationDelivery save(NotificationDelivery delivery) {
        NotificationDeliveryEntity saved = repository.save(mapper.toEntity(delivery));
        return mapper.toDomain(saved);
    }

    @Override
    public NotificationDelivery update(NotificationDelivery delivery) {
        NotificationDeliveryEntity updated = repository.save(mapper.toEntity(delivery));
        return mapper.toDomain(updated);
    }

    @Override
    public Optional<NotificationDelivery> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<NotificationDelivery> findByNotificationId(Long notificationId) {
        return repository.findByNotificationId(notificationId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<NotificationDelivery> findByNotificationIdAndChannelId(Long notificationId, Integer channelId) {
        return repository.findByNotificationIdAndChannelId(notificationId, channelId).map(mapper::toDomain);
    }

    @Override
    public Page<NotificationDelivery> findDeliveriesByUserId(Long userId, Boolean read, Long channelId, DeliveryStatus status, Pageable pageable) {
        return repository.findAll(
                NotificationDeliverySpecification.fromQuery(userId, read, channelId, status),
                pageable
        ).map(mapper::toDomain);
    }

    @Override
    public List<NotificationDelivery> findByStatusAndAttemptsLessThan(DeliveryStatus status, int maxAttempts) {
        return repository.findByDeliveryStatusAndAttemptsLessThan(status, maxAttempts).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
