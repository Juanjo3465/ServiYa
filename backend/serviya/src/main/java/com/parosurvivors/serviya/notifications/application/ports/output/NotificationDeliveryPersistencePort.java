package com.parosurvivors.serviya.notifications.application.ports.output;

import com.parosurvivors.serviya.notifications.domain.DeliveryStatus;
import com.parosurvivors.serviya.notifications.domain.NotificationDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface NotificationDeliveryPersistencePort {
    NotificationDelivery save(NotificationDelivery delivery);
    NotificationDelivery update(NotificationDelivery delivery);
    Optional<NotificationDelivery> findById(Long id);
    List<NotificationDelivery> findByNotificationId(Long notificationId);
    Optional<NotificationDelivery> findByNotificationIdAndChannelId(Long notificationId, Integer channelId);
    Page<NotificationDelivery> findDeliveriesByUserId(Long userId, Boolean read, Long channelId, DeliveryStatus status, Pageable pageable);

    /** Entregas en el estado dado con menos de {@code maxAttempts} intentos: candidatas a reintento programado. */
    List<NotificationDelivery> findByStatusAndAttemptsLessThan(DeliveryStatus status, int maxAttempts);
}
