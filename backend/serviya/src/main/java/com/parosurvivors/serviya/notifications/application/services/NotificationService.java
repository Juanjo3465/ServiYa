package com.parosurvivors.serviya.notifications.application.services;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationChannelServicePort;
import com.parosurvivors.serviya.notifications.application.ports.input.NotificationDeliveryServicePort;
import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.notifications.application.ports.output.NotificationPersistencePort;
import com.parosurvivors.serviya.notifications.domain.ChannelName;
import com.parosurvivors.serviya.notifications.domain.Notification;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
                       Set<ChannelName> channels, Map<String, String> protectedData) {
        Notification notification = createNotification(userId, type, title, message, entityType, entityId);

        // Canal por defecto INTERNAL cuando el llamador no especifica nada.
        Set<ChannelName> requested = (channels == null || channels.isEmpty())
                ? Set.of(ChannelName.INTERNAL)
                : channels;

        for (Long channelId : resolveChannelIds(requested)) {
            notificationDeliveryServicePort.deliver(notification.getId(), channelId, protectedData);
        }
    }

    /** Traduce los nombres de canal a sus ids reales leyendo el catálogo (única fuente de resolución). */
    private List<Long> resolveChannelIds(Set<ChannelName> channels) {
        Set<String> names = channels.stream().map(Enum::name).collect(Collectors.toSet());
        return notificationChannelServicePort.getChannels().stream()
                .filter(channel -> names.contains(channel.getName()))
                .map(channel -> channel.getId().longValue())
                .collect(Collectors.toList());
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
