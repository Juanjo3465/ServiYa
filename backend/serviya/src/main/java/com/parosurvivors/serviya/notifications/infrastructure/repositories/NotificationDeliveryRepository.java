package com.parosurvivors.serviya.notifications.infrastructure.repositories;

import com.parosurvivors.serviya.notifications.domain.DeliveryStatus;
import com.parosurvivors.serviya.notifications.infrastructure.entities.NotificationDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDeliveryEntity, Long>,
        JpaSpecificationExecutor<NotificationDeliveryEntity> {
    List<NotificationDeliveryEntity> findByNotificationId(Long notificationId);
    Optional<NotificationDeliveryEntity> findByNotificationIdAndChannelId(Long notificationId, Integer channelId);
    List<NotificationDeliveryEntity> findByDeliveryStatusAndAttemptsLessThan(DeliveryStatus deliveryStatus, Integer attempts);

    @Query("SELECT nd FROM NotificationDeliveryEntity nd " +
           "JOIN NotificationEntity n ON nd.notificationId = n.id " +
           "WHERE nd.id = :id AND n.userId = :userId")
    Optional<NotificationDeliveryEntity> findByIdAndUserId(Long id, Long userId);
}
