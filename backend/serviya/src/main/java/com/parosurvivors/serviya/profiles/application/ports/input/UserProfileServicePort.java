package com.parosurvivors.serviya.profiles.application.ports.input;

import com.parosurvivors.serviya.profiles.application.dto.PatchProfileRequest;
import com.parosurvivors.serviya.profiles.application.dto.UserProfileResponse;

/**
 * Puerto de entrada de UserProfileService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 2).
 */
public interface UserProfileServicePort {

    UserProfileResponse getProfileInfo(Long userId);

    void patchProfile(Long userId, PatchProfileRequest dto);

    void updateMainAddress(Long userId, Long addressId);
}
