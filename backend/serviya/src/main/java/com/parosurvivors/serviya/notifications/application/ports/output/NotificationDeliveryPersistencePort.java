package com.parosurvivors.serviya.notifications.application.ports.output;

import com.parosurvivors.serviya.notifications.domain.NotificationDelivery;

import java.util.List;
import java.util.Optional;

public interface NotificationDeliveryPersistencePort {
    NotificationDelivery save(NotificationDelivery delivery);
    NotificationDelivery update(NotificationDelivery delivery);
    Optional<NotificationDelivery> findById(Long id);
    List<NotificationDelivery> findByNotificationId(Long notificationId);
    Optional<NotificationDelivery> findByNotificationIdAndChannelId(Long notificationId, Integer channelId);
}
