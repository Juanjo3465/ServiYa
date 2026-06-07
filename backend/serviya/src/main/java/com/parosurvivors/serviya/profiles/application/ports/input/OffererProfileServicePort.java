package com.parosurvivors.serviya.profiles.application.ports.input;

import com.parosurvivors.serviya.profiles.application.dto.OffererProfilePublicResponse;
import com.parosurvivors.serviya.profiles.application.dto.OffererProfileSummaryResponse;
import com.parosurvivors.serviya.profiles.application.dto.PatchOffererProfileRequest;

/**
 * Puerto de entrada de OffererProfileService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 2).
 */
public interface OffererProfileServicePort {

    OffererProfilePublicResponse getPublicProfile(Long userId);

    OffererProfileSummaryResponse getProfileSummary(Long userId);

    void patchOffererProfile(Long userId, PatchOffererProfileRequest dto);
}
