package com.parosurvivors.serviya.requests.infrastructure.adapters.input;

import com.parosurvivors.serviya.requests.application.ports.input.RequestMaintenanceServicePort;
import com.parosurvivors.serviya.shared.lock.DistributedLock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada por TIEMPO: dispara las tareas de mantenimiento de solicitudes/propuestas
 * ({@link RequestMaintenanceServicePort}) en un cron configurable. Solo enruta hacia el puerto de
 * entrada (sin lógica), análogo a un controller REST pero con {@code @Scheduled} como disparador.
 * Requiere {@code @EnableScheduling} (activado en la clase de arranque). Crons por defecto: cada hora.
 *
 * <p>Cada tarea se envuelve en un {@link DistributedLock} (clave {@code lock:{sched:...}}) para que, con
 * ≥2 instancias, solo una la ejecute por disparo (deuda #1). Son idempotentes: si Redis falla, la tarea
 * se omite (D-08) y se retoma en el siguiente cron. El TTL es la red de seguridad si la instancia muere
 * a mitad; holgado frente a la duración real (ver catálogo de claves).</p>
 */
@Component
@RequiredArgsConstructor
public class RequestMaintenanceScheduler {

    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    private final RequestMaintenanceServicePort maintenanceService;
    private final DistributedLock distributedLock;

    @Scheduled(cron = "${serviya.maintenance.cron.reject-expired-pending:0 0 * * * *}")
    public void rejectExpiredPendingRequests() {
        distributedLock.runIfAcquired("sched:reject-expired-pending", LOCK_TTL,
                maintenanceService::rejectExpiredPendingRequests);
    }

    @Scheduled(cron = "${serviya.maintenance.cron.mark-stale-accepted:0 10 * * * *}")
    public void markStaleAcceptedAsNotProvided() {
        distributedLock.runIfAcquired("sched:mark-stale-accepted", LOCK_TTL,
                maintenanceService::markStaleAcceptedAsNotProvided);
    }

    @Scheduled(cron = "${serviya.maintenance.cron.reject-expired-proposals:0 20 * * * *}")
    public void rejectExpiredProposals() {
        distributedLock.runIfAcquired("sched:reject-expired-proposals", LOCK_TTL,
                maintenanceService::rejectExpiredProposals);
    }

    @Scheduled(cron = "${serviya.maintenance.cron.finalize-unconfirmed:0 30 * * * *}")
    public void finalizeUnconfirmedCompletions() {
        distributedLock.runIfAcquired("sched:finalize-unconfirmed", LOCK_TTL,
                maintenanceService::finalizeUnconfirmedCompletions);
    }
}
