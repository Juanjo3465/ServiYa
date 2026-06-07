package com.parosurvivors.serviya.users.application.ports.input;

import com.parosurvivors.serviya.users.application.dto.AuthResponse;
import com.parosurvivors.serviya.users.application.dto.RegisterRequest;

/**
 * Puerto de entrada de UserAuthenticationService — orquestador de login, registro
 * y recuperación de contraseña.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 1).
 */
public interface UserAuthenticationServicePort {

    AuthResponse login(String email, String rawPassword);

    AuthResponse register(RegisterRequest dto, String roleName);

    void requestPasswordReset(String email);

    void confirmPasswordReset(String rawToken, String newPassword);
}
