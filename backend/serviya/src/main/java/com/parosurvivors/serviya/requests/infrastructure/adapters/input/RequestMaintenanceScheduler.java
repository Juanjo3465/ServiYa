package com.parosurvivors.serviya.requests.infrastructure.adapters.input;

import com.parosurvivors.serviya.requests.application.ports.input.RequestMaintenanceServicePort;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada por TIEMPO: dispara las tareas de mantenimiento de solicitudes/propuestas
 * ({@link RequestMaintenanceServicePort}) en un cron configurable. Solo enruta hacia el puerto de
 * entrada (sin lógica), análogo a un controller REST pero con {@code @Scheduled} como disparador.
 * Requiere {@code @EnableScheduling} (activado en la clase de arranque). Crons por defecto: cada hora.
 * <p>
 * Cada tarea usa {@link SchedulerLock} para evitar ejecución duplicada al escalar a varias instancias.
 * El nombre del lock es único por tarea; {@code lockAtMostFor} cubre el peor caso de duración.
 */
@Component
@RequiredArgsConstructor
public class RequestMaintenanceScheduler {

    private final RequestMaintenanceServicePort maintenanceService;

    @Scheduled(cron = "${serviya.maintenance.cron.reject-expired-pending:0 0 * * * *}")
    @SchedulerLock(name = "rejectExpiredPendingRequests", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void rejectExpiredPendingRequests() {
        maintenanceService.rejectExpiredPendingRequests();
    }

    @Scheduled(cron = "${serviya.maintenance.cron.mark-stale-accepted:0 10 * * * *}")
    @SchedulerLock(name = "markStaleAcceptedAsNotProvided", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void markStaleAcceptedAsNotProvided() {
        maintenanceService.markStaleAcceptedAsNotProvided();
    }

    @Scheduled(cron = "${serviya.maintenance.cron.reject-expired-proposals:0 20 * * * *}")
    @SchedulerLock(name = "rejectExpiredProposals", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void rejectExpiredProposals() {
        maintenanceService.rejectExpiredProposals();
    }

    @Scheduled(cron = "${serviya.maintenance.cron.finalize-unconfirmed:0 30 * * * *}")
    @SchedulerLock(name = "finalizeUnconfirmedCompletions", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void finalizeUnconfirmedCompletions() {
        maintenanceService.finalizeUnconfirmedCompletions();
    }
}
