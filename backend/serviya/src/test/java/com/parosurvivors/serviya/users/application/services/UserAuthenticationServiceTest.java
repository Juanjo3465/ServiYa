package com.parosurvivors.serviya.users.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.shared.exceptions.UnauthorizedException;
import com.parosurvivors.serviya.users.application.dto.command.LoginCommand;
import com.parosurvivors.serviya.users.application.dto.result.AuthResult;
import com.parosurvivors.serviya.users.application.dto.result.IssuedToken;
import com.parosurvivors.serviya.users.application.ports.input.PasswordResetTokenServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserCreationServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserRoleServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserServicePort;
import com.parosurvivors.serviya.users.application.ports.output.TokenProviderPort;
import com.parosurvivors.serviya.users.application.ports.output.UserPersistencePort;
import com.parosurvivors.serviya.users.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Reglas de HU-001 (login): valida bcrypt y rechaza cuentas baneadas o eliminadas.
 */
@ExtendWith(MockitoExtension.class)
class UserAuthenticationServiceTest {

    @Mock UserServicePort userServicePort;
    @Mock UserCreationServicePort userCreationServicePort;
    @Mock PasswordResetTokenServicePort passwordResetTokenServicePort;
    @Mock UserPersistencePort userPersistencePort;
    @Mock NotificationServicePort notificationServicePort;
    @Mock UserRoleServicePort userRoleServicePort;
    @Mock PasswordEncoder passwordEncoder;
    @Mock TokenProviderPort tokenProvider;

    @InjectMocks UserAuthenticationService service;

    private final LoginCommand command = new LoginCommand("user@example.com", "raw-password");

    private User activeUser() {
        return User.builder().id(1L).email("user@example.com").passwordHash("$2a$hash").banned(false).build();
    }

    @Test
    void login_succeeds_withValidCredentials() {
        when(userPersistencePort.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser()));
        when(passwordEncoder.matches("raw-password", "$2a$hash")).thenReturn(true);
        when(userRoleServicePort.getUserRoles(1L)).thenReturn(List.of());
        when(tokenProvider.issue(anyLong(), anyList()))
                .thenReturn(new IssuedToken("jwt-token", LocalDateTime.now().plusHours(1)));

        AuthResult result = service.login(command);

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.userId()).isEqualTo(1L);
    }

    @Test
    void login_fails_whenEmailNotFound() {
        when(userPersistencePort.findByEmail("user@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(command)).isInstanceOf(UnauthorizedException.class);
        verify(tokenProvider, never()).issue(anyLong(), anyList());
    }

    @Test
    void login_fails_whenPasswordDoesNotMatch() {
        when(userPersistencePort.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser()));
        when(passwordEncoder.matches("raw-password", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(command)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_fails_whenUserIsBanned() {
        User banned = activeUser();
        banned.ban();
        when(userPersistencePort.findByEmail("user@example.com")).thenReturn(Optional.of(banned));
        when(passwordEncoder.matches("raw-password", "$2a$hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login(command)).isInstanceOf(UnauthorizedException.class);
        verify(tokenProvider, never()).issue(anyLong(), anyList());
    }

    @Test
    void login_fails_whenUserIsSoftDeleted() {
        User deleted = activeUser();
        deleted.softDelete();
        when(userPersistencePort.findByEmail("user@example.com")).thenReturn(Optional.of(deleted));
        when(passwordEncoder.matches("raw-password", "$2a$hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login(command)).isInstanceOf(UnauthorizedException.class);
        verify(tokenProvider, never()).issue(anyLong(), anyList());
    }
}
