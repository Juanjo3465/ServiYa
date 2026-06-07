package com.parosurvivors.serviya.users.application.ports.input;

import com.parosurvivors.serviya.users.domain.User;

/**
 * Puerto de entrada de UserService — gestión del usuario ya existente.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 1).
 */
public interface UserServicePort {

    User createUser(String email, String rawPassword);

    void changePassword(int userId, String currentRaw, String newRaw);

    void changeEmail(int userId, String newEmail);

    void banUser(int userId);

    void unbanUser(int userId);
}
