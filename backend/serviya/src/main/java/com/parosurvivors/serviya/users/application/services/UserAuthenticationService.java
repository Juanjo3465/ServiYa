package com.parosurvivors.serviya.users.application.services;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.users.application.dto.command.ConfirmPasswordResetCommand;
import com.parosurvivors.serviya.users.application.dto.command.LoginCommand;
import com.parosurvivors.serviya.users.application.dto.command.RegisterUserCommand;
import com.parosurvivors.serviya.users.application.dto.command.RequestPasswordResetCommand;
import com.parosurvivors.serviya.users.application.dto.result.AuthResult;
import com.parosurvivors.serviya.users.application.ports.input.PasswordResetTokenServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserAuthenticationServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserCreationServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserServicePort;
import com.parosurvivors.serviya.users.application.ports.output.UserPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de UserAuthenticationServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class UserAuthenticationService implements UserAuthenticationServicePort {

    private final UserServicePort userServicePort;
    private final UserCreationServicePort userCreationServicePort;
    private final PasswordResetTokenServicePort passwordResetTokenServicePort;
    private final UserPersistencePort userPersistencePort;
    private final NotificationServicePort notificationServicePort;

    @Override
    public AuthResult login(LoginCommand command) {
        throw new UnsupportedOperationException("TODO: login — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public AuthResult register(RegisterUserCommand command) {
        throw new UnsupportedOperationException("TODO: register — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void requestPasswordReset(RequestPasswordResetCommand command) {
        throw new UnsupportedOperationException("TODO: requestPasswordReset — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void confirmPasswordReset(ConfirmPasswordResetCommand command) {
        throw new UnsupportedOperationException("TODO: confirmPasswordReset — placeholder, ver estructura-servicios.docx");
    }
}
