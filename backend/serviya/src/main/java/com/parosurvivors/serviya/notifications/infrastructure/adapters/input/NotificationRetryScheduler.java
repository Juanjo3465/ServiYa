package com.parosurvivors.serviya.notifications.infrastructure.adapters.input;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationDeliveryServicePort;
import com.parosurvivors.serviya.shared.lock.DistributedLock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada por TIEMPO: reintenta periódicamente las entregas de notificación que fallaron
 * y aún no agotaron el máximo de intentos ({@code serviya.notifications.max-delivery-attempts}). Solo
 * enruta hacia el puerto de entrada, análogo a {@code RequestMaintenanceScheduler}. Requiere
 * {@code @EnableScheduling} (activado en la clase de arranque). Cron por defecto: cada 15 minutos.
 *
 * <p>Envuelto en {@link DistributedLock} (clave {@code lock:{sched:notif-retry-failed}}) para que, con
 * ≥2 instancias, solo una ejecute el reintento por disparo (deuda #1). Si Redis falla, se omite (D-08).</p>
 */
@Component
@RequiredArgsConstructor
public class NotificationRetryScheduler {

    /** Recurso del lock distribuido (clave Redis {@code lock:{sched:notif-retry-failed}}; ver catálogo). */
    private static final String LOCK_RETRY_FAILED = "sched:notif-retry-failed";
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    private final NotificationDeliveryServicePort notificationDeliveryService;
    private final DistributedLock distributedLock;

    @Scheduled(cron = "${serviya.notifications.cron.retry-failed:0 */15 * * * *}")
    public void retryFailedDeliveries() {
        distributedLock.runIfAcquired(LOCK_RETRY_FAILED, LOCK_TTL,
                notificationDeliveryService::retryFailedDeliveries);
    }
}
