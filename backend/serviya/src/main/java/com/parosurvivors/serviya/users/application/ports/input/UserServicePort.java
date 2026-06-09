package com.parosurvivors.serviya.users.application.ports.input;

import com.parosurvivors.serviya.users.application.dto.command.ChangeEmailCommand;
import com.parosurvivors.serviya.users.application.dto.command.ChangePasswordCommand;
import com.parosurvivors.serviya.users.domain.User;

/**
 * Puerto de entrada de UserService — gestión del usuario ya existente.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 1).
 */
public interface UserServicePort {

    User createUser(String email, String rawPassword);

    void changePassword(ChangePasswordCommand command);

    void changeEmail(ChangeEmailCommand command);

    void banUser(Long userId);

    void unbanUser(Long userId);
}
