package com.parosurvivors.serviya.notifications.application.services;

import com.parosurvivors.serviya.notifications.application.dto.result.NotificationDeliveryResult;
import com.parosurvivors.serviya.notifications.application.ports.input.NotificationDeliveryServicePort;
import com.parosurvivors.serviya.notifications.application.ports.output.EmailPort;
import com.parosurvivors.serviya.notifications.application.ports.output.NotificationChannelPersistencePort;
import com.parosurvivors.serviya.notifications.application.ports.output.NotificationDeliveryPersistencePort;
import com.parosurvivors.serviya.notifications.application.ports.output.NotificationPersistencePort;
import com.parosurvivors.serviya.notifications.domain.DeliveryStatus;
import com.parosurvivors.serviya.notifications.domain.Notification;
import com.parosurvivors.serviya.notifications.domain.NotificationDelivery;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationDeliveryService implements NotificationDeliveryServicePort {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryService.class);

    private final NotificationDeliveryPersistencePort notificationDeliveryPersistencePort;
    private final NotificationChannelPersistencePort notificationChannelPersistencePort;
    private final NotificationPersistencePort notificationPersistencePort;
    private final ObjectProvider<EmailPort> emailPortProvider;

    @Override
    public NotificationDelivery deliver(Long notificationId, Long channelId, Map<String, String> protectedData) {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .notificationId(notificationId)
                .channelId(channelId.intValue())
                .deliveryStatus(DeliveryStatus.PENDING)
                .build();

        notificationChannelPersistencePort.findById(channelId.intValue()).ifPresentOrElse(channel -> {
            switch (channel.getName()) {
                case "INTERNAL" -> {
                    delivery.markAsSent();
                }
                case "EMAIL" -> {
                    EmailPort emailPort = emailPortProvider.getIfUnique();
                    if (emailPort != null) {
                        Notification notification = notificationPersistencePort.findById(notificationId).orElse(null);
                        if (notification != null) {
                            boolean sent = emailPort.send(
                                    notification.getUserId(),
                                    notification.getNotificationType(),
                                    notification.getTitle(),
                                    notification.getMessage(),
                                    protectedData);
                            if (sent) {
                                delivery.markAsSent();
                            } else {
                                delivery.markAsFailed();
                            }
                        } else {
                            delivery.markAsFailed();
                        }
                    } else {
                        log.warn("No EmailPort bean available — marking EMAIL delivery {} as FAILED", notificationId);
                        delivery.markAsFailed();
                    }
                }
                default -> {
                    log.warn("Unknown channel '{}' — marking delivery {} as FAILED", channel.getName(), notificationId);
                    delivery.markAsFailed();
                }
            }
        }, () -> {
            log.warn("Channel ID {} not found — marking delivery {} as FAILED", channelId, notificationId);
            delivery.markAsFailed();
        });

        return notificationDeliveryPersistencePort.save(delivery);
    }

    @Override
    public Page<NotificationDeliveryResult> getDeliveries(Long userId, Boolean read, Long channelId, String status, Pageable pageable) {
        DeliveryStatus deliveryStatus = status != null ? DeliveryStatus.valueOf(status) : null;

        Page<NotificationDelivery> deliveries = notificationDeliveryPersistencePort
                .findDeliveriesByUserId(userId, read, channelId, deliveryStatus, pageable);

        List<Long> notifIds = deliveries.stream()
                .map(NotificationDelivery::getNotificationId)
                .distinct()
                .toList();

        Map<Long, Notification> notifMap = notificationPersistencePort.findAllById(notifIds).stream()
                .collect(Collectors.toMap(Notification::getId, Function.identity()));

        return deliveries.map(delivery -> {
            Notification notification = notifMap.get(delivery.getNotificationId());
            return new NotificationDeliveryResult(
                    delivery.getId(),
                    delivery.getNotificationId(),
                    delivery.getChannelId(),
                    delivery.getDeliveryStatus().name(),
                    delivery.getReadAt(),
                    delivery.getSentAt(),
                    notification != null ? notification.getNotificationType() : null,
                    notification != null ? notification.getTitle() : null,
                    notification != null ? notification.getMessage() : null,
                    notification != null ? notification.getEntityType() : null,
                    notification != null ? notification.getEntityId() : null,
                    notification != null ? notification.getCreatedAt() : null
            );
        });
    }

    @Override
    public String getDeliveryStatus(Long notificationId, Long channelId) {
        return notificationDeliveryPersistencePort
                .findByNotificationIdAndChannelId(notificationId, channelId.intValue())
                .map(d -> d.getDeliveryStatus().name())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Delivery not found for notification " + notificationId + " and channel " + channelId));
    }

    @Override
    public boolean isRead(Long notificationId, Long channelId) {
        return notificationDeliveryPersistencePort
                .findByNotificationIdAndChannelId(notificationId, channelId.intValue())
                .map(NotificationDelivery::isRead)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Delivery not found for notification " + notificationId + " and channel " + channelId));
    }

    @Override
    public void markAsRead(Long deliveryId) {
        NotificationDelivery delivery = notificationDeliveryPersistencePort.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found: " + deliveryId));
        delivery.markAsRead();
        notificationDeliveryPersistencePort.update(delivery);
    }
}
