package com.parosurvivors.serviya.users.application.services;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.shared.exceptions.UnauthorizedException;
import com.parosurvivors.serviya.users.application.dto.command.ConfirmPasswordResetCommand;
import com.parosurvivors.serviya.users.application.dto.command.CreateUserAccountCommand;
import com.parosurvivors.serviya.users.application.dto.command.LoginCommand;
import com.parosurvivors.serviya.users.application.dto.command.RegisterUserCommand;
import com.parosurvivors.serviya.users.application.dto.command.RequestPasswordResetCommand;
import com.parosurvivors.serviya.users.application.dto.result.AuthResult;
import com.parosurvivors.serviya.users.application.dto.result.IssuedToken;
import com.parosurvivors.serviya.users.application.ports.input.PasswordResetTokenServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserAuthenticationServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserCreationServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserRoleServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserServicePort;
import com.parosurvivors.serviya.users.application.ports.output.TokenProviderPort;
import com.parosurvivors.serviya.users.application.ports.output.UserPersistencePort;
import com.parosurvivors.serviya.users.domain.Role;
import com.parosurvivors.serviya.users.domain.RoleName;
import com.parosurvivors.serviya.users.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Orquestador de autenticacion (RF-001 login, RF-002 registro). El registro delega la creacion
 * comun en {@link UserCreationServicePort}. La emision del token va por {@link TokenProviderPort}
 * (detalle JWT en infraestructura). Recuperacion de contrasena sigue pendiente (placeholder).
 */
@Component
@RequiredArgsConstructor
public class UserAuthenticationService implements UserAuthenticationServicePort {

    private final UserServicePort userServicePort;
    private final UserCreationServicePort userCreationServicePort;
    private final PasswordResetTokenServicePort passwordResetTokenServicePort;
    private final UserPersistencePort userPersistencePort;
    private final NotificationServicePort notificationServicePort;
    private final UserRoleServicePort userRoleServicePort;
    private final PasswordEncoder passwordEncoder;
    private final TokenProviderPort tokenProvider;

    @Override
    public AuthResult login(LoginCommand command) {
        // Mensaje generico para no revelar si el email existe.
        User user = userPersistencePort.findByEmail(command.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        // RF-001 / RNF-007: cuentas baneadas o eliminadas no pueden iniciar sesion.
        if (user.isBanned()) {
            throw new UnauthorizedException("Account is banned");
        }
        if (user.isDeleted()) {
            throw new UnauthorizedException("Account no longer exists");
        }

        List<RoleName> roles = userRoleServicePort.getUserRoles(user.getId()).stream()
                .map(Role::getName)
                .toList();

        return toAuthResult(user.getId(), roles);
    }

    @Override
    public AuthResult register(RegisterUserCommand command) {
        CreateUserAccountCommand accountCommand = new CreateUserAccountCommand(
                command.email(),
                command.password(),
                command.fullName(),
                command.role(),
                command.documentType(),
                command.documentNumber(),
                command.phone(),
                command.acceptedTerms());

        User created = userCreationServicePort.createUserAccount(accountCommand);
        return toAuthResult(created.getId(), created.getRoles());
    }

    @Override
    public void requestPasswordReset(RequestPasswordResetCommand command) {
        throw new UnsupportedOperationException("TODO: requestPasswordReset — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void confirmPasswordReset(ConfirmPasswordResetCommand command) {
        throw new UnsupportedOperationException("TODO: confirmPasswordReset — placeholder, ver estructura-servicios.docx");
    }

    private AuthResult toAuthResult(Long userId, List<RoleName> roles) {
        IssuedToken issued = tokenProvider.issue(userId, roles);
        return new AuthResult(issued.token(), userId, roles, issued.expiresAt());
    }
}
