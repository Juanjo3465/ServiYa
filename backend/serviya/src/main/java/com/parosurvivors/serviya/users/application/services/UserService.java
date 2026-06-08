package com.parosurvivors.serviya.users.application.services;

import com.parosurvivors.serviya.users.application.ports.input.UserServicePort;
import com.parosurvivors.serviya.users.application.ports.output.UserPersistencePort;
import com.parosurvivors.serviya.users.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de UserServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class UserService implements UserServicePort {

    private final UserPersistencePort userPersistencePort;

    @Override
    public User createUser(String email, String rawPassword) {
        throw new UnsupportedOperationException("TODO: createUser — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void changePassword(Long userId, String currentRaw, String newRaw) {
        throw new UnsupportedOperationException("TODO: changePassword — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void changeEmail(Long userId, String newEmail) {
        throw new UnsupportedOperationException("TODO: changeEmail — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void banUser(Long userId) {
        throw new UnsupportedOperationException("TODO: banUser — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void unbanUser(Long userId) {
        throw new UnsupportedOperationException("TODO: unbanUser — placeholder, ver estructura-servicios.docx");
    }
}
