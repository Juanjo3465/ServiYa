package com.parosurvivors.serviya.users.application.services;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestCommandServicePort;
import com.parosurvivors.serviya.services.application.ports.input.MarketplaceServicePort;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import com.parosurvivors.serviya.users.application.ports.input.UserDeletionServicePort;
import com.parosurvivors.serviya.users.application.ports.output.UserPersistencePort;
import com.parosurvivors.serviya.users.application.ports.output.UserReadPort;
import com.parosurvivors.serviya.users.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestador de la eliminación (soft delete) de un usuario que puede ser oferente y/o cliente
 * con solicitudes activas. Evita solicitudes huérfanas: desactiva los servicios del oferente y
 * cancela las solicitudes activas donde participa. Todo atómico (@Transactional).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 1).
 */
@Component
@RequiredArgsConstructor
public class UserDeletionService implements UserDeletionServicePort {

    private final UserPersistencePort userPersistencePort;
    private final UserReadPort userReadPort;
    private final MarketplaceServicePort marketplaceServicePort;
    private final ServiceRequestCommandServicePort serviceRequestCommandServicePort;
    private final NotificationServicePort notificationServicePort;

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userReadPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (user.isDeleted()) {
            return; // idempotente: la cuenta ya está eliminada.
        }
        // (1) Soft delete de la cuenta.
        user.softDelete();
        userPersistencePort.update(user);
        // (2) Si es oferente, ocultar todos sus servicios del buscador (no-op si no tiene servicios).
        marketplaceServicePort.deactivateAllByOfferer(userId);
        // (3) Cancelar las solicitudes activas donde participa (como cliente u oferente) para no dejar huérfanas.
        serviceRequestCommandServicePort.cancelActiveRequestsForUser(userId);
        // TODO(notif): notificar por doble canal a las contrapartes de las solicitudes canceladas (RF).
    }
}
