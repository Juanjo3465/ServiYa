package com.parosurvivors.serviya.notifications.application.ports.output;

import com.parosurvivors.serviya.notifications.domain.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationPersistencePort {
    Notification save(Notification notification);
    Optional<Notification> findById(Long id);
    List<Notification> findByUserId(Long userId);
    List<Notification> findAllById(List<Long> ids);
}
