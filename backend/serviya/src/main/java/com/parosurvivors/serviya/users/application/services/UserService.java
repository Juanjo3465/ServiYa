package com.parosurvivors.serviya.users.application.services;

import com.parosurvivors.serviya.shared.exceptions.InvalidStateException;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import com.parosurvivors.serviya.users.application.dto.command.ChangeEmailCommand;
import com.parosurvivors.serviya.users.application.dto.command.ChangePasswordCommand;
import com.parosurvivors.serviya.users.application.ports.input.UserServicePort;
import com.parosurvivors.serviya.users.application.ports.output.UserPersistencePort;
import com.parosurvivors.serviya.users.application.ports.output.UserReadPort;
import com.parosurvivors.serviya.users.domain.User;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final UserReadPort userReadPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User createUser(String email, String rawPassword) {
        if (userReadPort.existsByEmail(email)) {
            throw new InvalidStateException("Email is already registered: " + email);
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .banned(false)
                .createdAt(LocalDateTime.now())
                .build();
        return userPersistencePort.save(user);
    }

    @Override
    public void changePassword(ChangePasswordCommand command) {
        User user = userReadPort.findById(command.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + command.userId()));
        
        if(!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.changePassword(passwordEncoder.encode(command.newPassword()));
        userPersistencePort.update(user);
    }

    @Override
    public void changeEmail(ChangeEmailCommand command) {
        //Checks for the existense of the user and if the email is already taken before updating the email.
        User user = userReadPort.findById(command.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + command.userId()));
        if (userReadPort.findByEmail(command.newEmail()).isPresent()) {
            throw new InvalidStateException("Email is already taken: " + command.newEmail());
        }
        user.changeEmail(command.newEmail());
        userPersistencePort.update(user);
    }

    @Override
    public void banUser(Long userId) {
        User user = userReadPort.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.ban();
        userPersistencePort.update(user);
        // TODO(notif): notificar al usuario baneado por doble canal con motivos (RF-063).
    }

    @Override
    public void unbanUser(Long userId) {
        User user = userReadPort.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.unban();
        userPersistencePort.update(user);
        // TODO(notif): notificar al usuario desbaneado por doble canal (RF-075).
    }
}
