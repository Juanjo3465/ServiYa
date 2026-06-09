package com.parosurvivors.serviya.notifications.infrastructure.repositories;

import com.parosurvivors.serviya.notifications.infrastructure.entities.NotificationDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDeliveryEntity, Long> {
    List<NotificationDeliveryEntity> findByNotificationId(Long notificationId);
    Optional<NotificationDeliveryEntity> findByNotificationIdAndChannelId(Long notificationId, Integer channelId);
}
