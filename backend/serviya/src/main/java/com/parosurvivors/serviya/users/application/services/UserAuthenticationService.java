package com.parosurvivors.serviya.users.application.services;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.shared.exceptions.InvalidStateException;
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
import com.parosurvivors.serviya.users.application.ports.output.UserReadPort;
import com.parosurvivors.serviya.users.domain.Role;
import com.parosurvivors.serviya.users.domain.RoleName;
import com.parosurvivors.serviya.users.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserReadPort userReadPort;
    private final NotificationServicePort notificationServicePort;
    private final UserRoleServicePort userRoleServicePort;
    private final PasswordEncoder passwordEncoder;
    private final TokenProviderPort tokenProvider;

    @Override
    public AuthResult login(LoginCommand command) {
        // Mensaje generico para no revelar si el email existe.
        User user = userReadPort.findByEmail(command.email())
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
        // Restricción de POLÍTICA del registro público: solo CLIENT u OFFERER, nunca ADMIN. El mecanismo
        // interno de creación (createUserAccount) no restringe roles; la política vive en este llamador.
        requirePublicRole(command.role());
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

    /**
     * RF-010/011: adquiere el rol y re-emite el JWT con los roles ya actualizados.
     *
     * <p>Atomica: la asignacion del rol y la inicializacion de sus filas 1-a-1 (perfil de oferente,
     * metricas — que reaccionan a RoleAssignedEvent en BEFORE_COMMIT) ocurren en esta transaccion.
     * El token se emite despues de releer los roles, por lo que el usuario obtiene acceso inmediato
     * sin volver a iniciar sesion.</p>
     */
    @Override
    @Transactional
    public AuthResult acquireRole(Long userId, String roleName) {
        userRoleServicePort.acquireRole(userId, roleName);

        List<RoleName> roles = userRoleServicePort.getUserRoles(userId).stream()
                .map(Role::getName)
                .toList();

        return toAuthResult(userId, roles);
    }

    @Override
    public void requestPasswordReset(RequestPasswordResetCommand command) {
        throw new UnsupportedOperationException("TODO: requestPasswordReset — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void confirmPasswordReset(ConfirmPasswordResetCommand command) {
        throw new UnsupportedOperationException("TODO: confirmPasswordReset — placeholder, ver estructura-servicios.docx");
    }

    /** El registro público solo permite roles CLIENT u OFFERER; ADMIN nunca por esta vía. */
    private void requirePublicRole(String role) {
        RoleName roleName;
        try {
            roleName = RoleName.valueOf(role == null ? "" : role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidStateException("Invalid role: " + role);
        }
        if (roleName == RoleName.ADMIN) {
            throw new InvalidStateException("Public registration cannot create ADMIN accounts");
        }
    }

    private AuthResult toAuthResult(Long userId, List<RoleName> roles) {
        IssuedToken issued = tokenProvider.issue(userId, roles);
        return new AuthResult(issued.token(), userId, roles, issued.expiresAt());
    }
}
