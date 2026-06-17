package com.parosurvivors.serviya.users.application.services;

import com.parosurvivors.serviya.profiles.application.dto.command.CreateUserProfileCommand;
import com.parosurvivors.serviya.profiles.application.ports.input.UserProfileServicePort;
import com.parosurvivors.serviya.profiles.domain.ProfileType;
import com.parosurvivors.serviya.shared.exceptions.InvalidStateException;
import com.parosurvivors.serviya.users.application.dto.command.CreateUserAccountCommand;
import com.parosurvivors.serviya.users.application.ports.input.ConsentServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserCreationServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserRoleServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserServicePort;
import com.parosurvivors.serviya.users.domain.RoleName;
import com.parosurvivors.serviya.users.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flujo compartido de creacion de usuario (RF-002), reutilizable por el registro publico.
 * Es atomico ({@link Transactional}): credenciales + rol + perfil + consentimiento se crean
 * en una sola transaccion; si algo falla, no queda informacion parcial.
 *
 * <p>Reglas de negocio aplicadas:
 * <ul>
 *   <li>RF-004: sin consentimiento explicito no se crea la cuenta.</li>
 *   <li>El registro publico solo permite roles CLIENT u OFFERER, nunca ADMIN.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class UserCreationService implements UserCreationServicePort {

    private final UserServicePort userServicePort;
    private final UserRoleServicePort userRoleServicePort;
    private final ConsentServicePort consentServicePort;
    private final UserProfileServicePort userProfileServicePort;

    @Override
    @Transactional
    public User createUserAccount(CreateUserAccountCommand command) {
        // RF-004: el consentimiento debe ser explicito; si falta o es false, no se crea la cuenta.
        if (command.acceptedTerms() == null || !command.acceptedTerms()) {
            throw new InvalidStateException("Data usage consent is required to create an account");
        }

        // Solo roles publicos validos (CLIENT u OFFERER), nunca ADMIN.
        RoleName roleName = parsePublicRole(command.role());

        // Credenciales (valida email unico y cifra la contrasena con bcrypt).
        User user = userServicePort.createUser(command.email(), command.password());

        // Rol elegido.
        userRoleServicePort.acquireRole(user.getId(), roleName.name());

        // Perfil personal (document/phone se cifran al persistir).
        userProfileServicePort.createProfile(new CreateUserProfileCommand(
                user.getId(),
                command.fullName(),
                nullToEmpty(command.documentType()),
                nullToEmpty(command.documentNumber()),
                nullToEmpty(command.phone()),
                ProfileType.NATURAL));

        // RF-004: registro del consentimiento aceptado.
        consentServicePort.createConsent(user.getId(), true);

        user.setRoles(List.of(roleName));
        return user;
    }

    private RoleName parsePublicRole(String role) {
        RoleName roleName;
        try {
            roleName = RoleName.valueOf(role == null ? "" : role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidStateException("Invalid role: " + role);
        }
        if (roleName == RoleName.ADMIN) {
            throw new InvalidStateException("Public registration cannot create ADMIN accounts");
        }
        return roleName;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
