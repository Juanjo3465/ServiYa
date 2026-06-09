package com.parosurvivors.serviya.users.application.ports.input;

import com.parosurvivors.serviya.users.application.dto.command.ConfirmPasswordResetCommand;
import com.parosurvivors.serviya.users.application.dto.command.LoginCommand;
import com.parosurvivors.serviya.users.application.dto.command.RegisterUserCommand;
import com.parosurvivors.serviya.users.application.dto.command.RequestPasswordResetCommand;
import com.parosurvivors.serviya.users.application.dto.result.AuthResult;

/**
 * Puerto de entrada de UserAuthenticationService — orquestador de login, registro
 * y recuperación de contraseña. Recibe Commands (capa aplicación) y devuelve AuthResult;
 * nunca tipos web (Form/Response).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 1).
 */
public interface UserAuthenticationServicePort {

    AuthResult login(LoginCommand command);

    AuthResult register(RegisterUserCommand command);

    void requestPasswordReset(RequestPasswordResetCommand command);

    void confirmPasswordReset(ConfirmPasswordResetCommand command);
}
