package com.parosurvivors.serviya.users.infrastructure.adapters.input;

import com.parosurvivors.serviya.users.application.ports.input.PasswordResetTokenServicePort;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada por TIEMPO: borra los tokens de recuperación ya caducados (RF-003).
 *
 * <p>Un token expirado no sirve para nada — ni siquiera para auditoría, porque su valor en claro nunca
 * se guardó — y dejarlos acumularse solo hace crecer la tabla y su índice único. Fino: solo enruta hacia
 * el puerto de entrada, igual que {@code NotificationRetryScheduler} o {@code RequestMaintenanceScheduler}.
 * Requiere {@code @EnableScheduling} (ya activado en la clase de arranque).</p>
 *
 * <p>Por defecto se ejecuta a diario de madrugada: es mantenimiento, no tiene ninguna urgencia (un token
 * caducado ya lo rechaza la validación aunque siga en la tabla).</p>
 */
@Component
@RequiredArgsConstructor
public class PasswordResetCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetCleanupScheduler.class);

    private final PasswordResetTokenServicePort passwordResetTokenService;

    @Scheduled(cron = "${serviya.password-reset.cron.cleanup-expired:0 30 3 * * *}")
    public void deleteExpiredTokens() {
        int deleted = passwordResetTokenService.deleteExpiredTokens(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Deleted {} expired password reset tokens", deleted);
        }
    }
}
