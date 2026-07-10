package com.parosurvivors.serviya.notifications.application.services;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationChannelServicePort;
import com.parosurvivors.serviya.notifications.application.ports.input.NotificationDeliveryServicePort;
import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.notifications.application.ports.output.NotificationPersistencePort;
import com.parosurvivors.serviya.notifications.domain.Notification;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationService implements NotificationServicePort {

    private final NotificationPersistencePort notificationPersistencePort;
    private final NotificationDeliveryServicePort notificationDeliveryServicePort;
    private final NotificationChannelServicePort notificationChannelServicePort;

    @Override
    @Transactional
    public void notify(Long userId, String type, String title, String message, String entityType, Long entityId,
                       List<Long> channelIds, Map<String, String> protectedData) {
        Notification notification = createNotification(userId, type, title, message, entityType, entityId);

        if (channelIds == null || channelIds.isEmpty()) {
            channelIds = notificationChannelServicePort.getChannels().stream()
                    .filter(c -> "INTERNAL".equals(c.getName()))
                    .map(c -> c.getId().longValue())
                    .collect(Collectors.toList());
        }

        for (Long channelId : channelIds) {
            notificationDeliveryServicePort.deliver(notification.getId(), channelId, protectedData);
        }
    }

    @Override
    public Notification createNotification(Long userId, String type, String title, String message,
                                           String entityType, Long entityId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .notificationType(type)
                .title(title)
                .message(message)
                .entityType(entityType)
                .entityId(entityId)
                .createdAt(LocalDateTime.now())
                .build();
        return notificationPersistencePort.save(notification);
    }
}
