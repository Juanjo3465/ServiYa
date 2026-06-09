package com.parosurvivors.serviya.profiles.application.services;

import com.parosurvivors.serviya.profiles.application.dto.command.UpdateProfileCommand;
import com.parosurvivors.serviya.profiles.application.ports.input.UserProfileServicePort;
import com.parosurvivors.serviya.profiles.application.ports.output.AddressPersistencePort;
import com.parosurvivors.serviya.profiles.application.ports.output.UserProfilePersistencePort;
import com.parosurvivors.serviya.profiles.domain.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de UserProfileServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class UserProfileService implements UserProfileServicePort {

    private final UserProfilePersistencePort userProfilePersistencePort;
    private final AddressPersistencePort addressPersistencePort;

    @Override
    public UserProfile getProfileInfo(Long userId) {
        throw new UnsupportedOperationException("TODO: getProfileInfo — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public UserProfile patchProfile(UpdateProfileCommand command) {
        throw new UnsupportedOperationException("TODO: patchProfile — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void updateMainAddress(Long userId, Long addressId) {
        throw new UnsupportedOperationException("TODO: updateMainAddress — placeholder, ver estructura-servicios.docx");
    }
}
