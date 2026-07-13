package com.parosurvivors.serviya.profiles.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parosurvivors.serviya.profiles.application.dto.command.UpdateProfileCommand;
import com.parosurvivors.serviya.profiles.application.ports.output.AddressPersistencePort;
import com.parosurvivors.serviya.profiles.application.ports.output.UserProfilePersistencePort;
import com.parosurvivors.serviya.profiles.domain.UserProfile;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock UserProfilePersistencePort userProfilePersistencePort;
    @Mock AddressPersistencePort addressPersistencePort;

    @InjectMocks UserProfileService service;

    @Test
    void patchesExistingUserProfile() {
        UserProfile existing = UserProfile.builder()
                .userId(11L)
                .fullName("Old")
                .phoneNumber("111")
                .bio("old")
                .build();

        when(userProfilePersistencePort.findByUserId(11L)).thenReturn(Optional.of(existing));
        when(userProfilePersistencePort.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile result = service.patchProfile(new UpdateProfileCommand(
                11L,
                "New Name",
                "222",
                "/uploads/profile.png",
                "new bio"));

        assertThat(result.getFullName()).isEqualTo("New Name");
        assertThat(result.getPhoneNumber()).isEqualTo("222");
        assertThat(result.getProfilePhotoUrl()).isEqualTo("/uploads/profile.png");
        assertThat(result.getBio()).isEqualTo("new bio");
        verify(userProfilePersistencePort).save(any(UserProfile.class));
    }
}
