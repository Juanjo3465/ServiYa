package com.parosurvivors.serviya.notifications.infrastructure.adapters.input;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationDeliveryServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada por TIEMPO: reintenta periódicamente las entregas de notificación que fallaron
 * y aún no agotaron el máximo de intentos ({@code serviya.notifications.max-delivery-attempts}). Solo
 * enruta hacia el puerto de entrada, análogo a {@code RequestMaintenanceScheduler}. Requiere
 * {@code @EnableScheduling} (activado en la clase de arranque). Cron por defecto: cada 15 minutos.
 */
@Component
@RequiredArgsConstructor
public class NotificationRetryScheduler {

    private final NotificationDeliveryServicePort notificationDeliveryService;

    @Scheduled(cron = "${serviya.notifications.cron.retry-failed:0 */15 * * * *}")
    public void retryFailedDeliveries() {
        notificationDeliveryService.retryFailedDeliveries();
    }
}
