package com.parosurvivors.serviya.notifications.infrastructure.adapters.output;

import com.parosurvivors.serviya.notifications.application.ports.output.RecipientEmailPort;
import com.parosurvivors.serviya.users.api.UserContactApi;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resuelve el correo del destinatario a través del puerto de ENTRADA de users (no de su persistencia, M8).
 * Es la única dependencia que queda del cliente de notificaciones hacia otro módulo, y vive en el monolito:
 * notifications-service recibe el correo ya resuelto.
 */
@Component
@RequiredArgsConstructor
public class UserRecipientEmailAdapter implements RecipientEmailPort {

    private final UserContactApi userContactApi;

    @Override
    public Optional<String> findEmail(Long userId) {
        // Un destinatario inexistente no debe tumbar la operación de negocio: la entrega EMAIL quedará FAILED.
        return userContactApi.emailOf(userId).filter(email -> !email.isBlank());
    }
}
