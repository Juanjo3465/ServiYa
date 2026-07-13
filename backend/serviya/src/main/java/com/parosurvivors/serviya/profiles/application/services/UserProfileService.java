package com.parosurvivors.serviya.profiles.application.services;

import com.parosurvivors.serviya.profiles.application.dto.command.CreateUserProfileCommand;
import com.parosurvivors.serviya.profiles.application.dto.command.UpdateProfileCommand;
import com.parosurvivors.serviya.profiles.application.ports.input.UserProfileServicePort;
import com.parosurvivors.serviya.profiles.application.ports.output.AddressPersistencePort;
import com.parosurvivors.serviya.profiles.application.ports.output.UserProfilePersistencePort;
import com.parosurvivors.serviya.profiles.domain.UserProfile;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Servicio del perfil personal. {@code createProfile} crea la fila de user_profiles durante el
 * registro (RF-002) y {@code getProfileInfo} expone la informacion del usuario autenticado
 * (RF-005). Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class UserProfileService implements UserProfileServicePort {

    private final UserProfilePersistencePort userProfilePersistencePort;
    private final AddressPersistencePort addressPersistencePort;

    @Override
    public UserProfile createProfile(CreateUserProfileCommand command) {
        UserProfile profile = UserProfile.builder()
                .userId(command.userId())
                .fullName(command.fullName())
                .documentType(command.documentType())
                .documentNumber(command.documentNumber())
                .phoneNumber(command.phoneNumber())
                .profileType(command.profileType())
                .createdAt(LocalDateTime.now())
                .build();
        return userProfilePersistencePort.save(profile);
    }

    @Override
    public UserProfile getProfileInfo(Long userId) {
        return userProfilePersistencePort.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));
    }

    @Override
    public UserProfile patchProfile(UpdateProfileCommand command) {
        throw new UnsupportedOperationException("TODO: patchProfile — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void updateMainAddress(Long userId, Long addressId) {
        Optional<UserProfile> userProfile = userProfilePersistencePort.findByUserId(userId);

        if (userProfile.isPresent()) {
            UserProfile profile = userProfile.get();
            profile.setPrimaryAddressId(addressId);
            userProfilePersistencePort.save(profile);
        }
    }
}
