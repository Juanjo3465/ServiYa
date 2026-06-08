package com.parosurvivors.serviya.notifications.infrastructure.adapters.input;

import com.parosurvivors.serviya.notifications.application.dto.NotificationDeliveryResponse;
import com.parosurvivors.serviya.notifications.application.ports.input.NotificationChannelServicePort;
import com.parosurvivors.serviya.notifications.application.ports.input.NotificationDeliveryServicePort;
import com.parosurvivors.serviya.notifications.domain.NotificationChannel;
import com.parosurvivors.serviya.notifications.infrastructure.adapters.input.api.NotificationApi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada (REST) de notificaciones. Placeholder funcional; documentacion en {@link NotificationApi}.
 */
@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationApi {

    private final NotificationDeliveryServicePort notificationDeliveryService;
    private final NotificationChannelServicePort notificationChannelService;

    @Override
    @GetMapping("/api/v1/notifications")
    public ResponseEntity<Page<NotificationDeliveryResponse>> getDeliveries(
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(notificationDeliveryService.getDeliveries(currentUserId(), read, channelId, status, pageable));
    }

    @Override
    @PostMapping("/api/v1/notifications/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationDeliveryService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/api/v1/notification-channels")
    public ResponseEntity<List<NotificationChannel>> getChannels() {
        return ResponseEntity.ok(notificationChannelService.getChannels());
    }

    /** TODO: reemplazar por el id extraido del JWT autenticado. */
    private Long currentUserId() {
        return 0L;
    }
}
