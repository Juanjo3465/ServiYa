package com.parosurvivors.serviya.users.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parosurvivors.serviya.profiles.application.dto.command.CreateUserProfileCommand;
import com.parosurvivors.serviya.profiles.application.ports.input.OffererProfileServicePort;
import com.parosurvivors.serviya.profiles.application.ports.input.UserProfileServicePort;
import com.parosurvivors.serviya.shared.exceptions.InvalidStateException;
import com.parosurvivors.serviya.users.application.dto.command.CreateUserAccountCommand;
import com.parosurvivors.serviya.users.application.ports.input.ConsentServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserRoleServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserServicePort;
import com.parosurvivors.serviya.users.domain.RoleName;
import com.parosurvivors.serviya.users.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Reglas de negocio de HU-002 (registro) y HU-004 (consentimiento) en el orquestador.
 */
@ExtendWith(MockitoExtension.class)
class UserCreationServiceTest {

    @Mock UserServicePort userServicePort;
    @Mock UserRoleServicePort userRoleServicePort;
    @Mock ConsentServicePort consentServicePort;
    @Mock UserProfileServicePort userProfileServicePort;
    @Mock OffererProfileServicePort offererProfileServicePort;

    @InjectMocks UserCreationService service;

    private CreateUserAccountCommand command(String role, Boolean accepted) {
        return new CreateUserAccountCommand(
                "new.user@example.com", "password123", "New User",
                role, "CC", "123456", "3001234567", accepted);
    }

    @Test
    void createsAccountAtomically_assigningRoleProfileAndConsent() {
        when(userServicePort.createUser(anyString(), anyString()))
                .thenReturn(User.builder().id(42L).email("new.user@example.com").build());

        User result = service.createUserAccount(command("CLIENT", true));

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getRoles()).containsExactly(RoleName.CLIENT);
        verify(userServicePort).createUser("new.user@example.com", "password123");
        verify(userRoleServicePort).acquireRole(42L, "CLIENT");
        verify(userProfileServicePort).createProfile(any(CreateUserProfileCommand.class));
        verify(consentServicePort).createConsent(42L, true);
    }

    @Test
    void rejectsRegistration_whenConsentNotAccepted() {
        assertThatThrownBy(() -> service.createUserAccount(command("CLIENT", false)))
                .isInstanceOf(InvalidStateException.class);

        // HU-004: nada se persiste si no hay consentimiento.
        verify(userServicePort, never()).createUser(anyString(), anyString());
        verify(consentServicePort, never()).createConsent(anyLong(), anyBoolean());
    }

    @Test
    void rejectsRegistration_whenConsentIsNull() {
        assertThatThrownBy(() -> service.createUserAccount(command("CLIENT", null)))
                .isInstanceOf(InvalidStateException.class);
        verify(userServicePort, never()).createUser(anyString(), anyString());
    }

    @Test
    void rejectsRegistration_whenRoleIsAdmin() {
        assertThatThrownBy(() -> service.createUserAccount(command("ADMIN", true)))
                .isInstanceOf(InvalidStateException.class);
        verify(userServicePort, never()).createUser(anyString(), anyString());
    }

    @Test
    void rejectsRegistration_whenRoleIsInvalid() {
        assertThatThrownBy(() -> service.createUserAccount(command("SUPERUSER", true)))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void allowsOffererRegistration() {
        when(userServicePort.createUser(anyString(), anyString()))
                .thenReturn(User.builder().id(7L).build());

        User result = service.createUserAccount(command("OFFERER", true));

        assertThat(result.getRoles()).containsExactly(RoleName.OFFERER);
        verify(userRoleServicePort).acquireRole(eq(7L), eq("OFFERER"));
        verify(offererProfileServicePort).createOffererProfile(7L);
    }
}
