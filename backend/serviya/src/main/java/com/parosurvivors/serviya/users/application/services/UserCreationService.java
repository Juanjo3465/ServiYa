package com.parosurvivors.serviya.users.application.services;

import com.parosurvivors.serviya.profiles.application.ports.input.UserProfileServicePort;
import com.parosurvivors.serviya.users.application.dto.command.CreateUserAccountCommand;
import com.parosurvivors.serviya.users.application.ports.input.ConsentServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserCreationServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserRoleServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserServicePort;
import com.parosurvivors.serviya.users.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de UserCreationServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class UserCreationService implements UserCreationServicePort {

    private final UserServicePort userServicePort;
    private final UserRoleServicePort userRoleServicePort;
    private final ConsentServicePort consentServicePort;
    private final UserProfileServicePort userProfileServicePort;

    @Override
    public User createUserAccount(CreateUserAccountCommand command) {
        throw new UnsupportedOperationException("TODO: createUserAccount — placeholder, ver estructura-servicios.docx");
    }
}
