package com.parosurvivors.serviya.users.application.services;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestCommandServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserDeletionServicePort;
import com.parosurvivors.serviya.users.application.ports.output.UserPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de UserDeletionServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class UserDeletionService implements UserDeletionServicePort {

    private final UserPersistencePort userPersistencePort;
    private final ServiceRequestCommandServicePort serviceRequestCommandServicePort;
    private final NotificationServicePort notificationServicePort;

    @Override
    public void deleteUser(Long userId) {
        throw new UnsupportedOperationException("TODO: deleteUser — placeholder, ver estructura-servicios.docx");
    }
}
