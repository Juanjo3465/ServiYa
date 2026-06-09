package com.parosurvivors.serviya.profiles.application.services;

import com.parosurvivors.serviya.profiles.application.dto.command.UpdateOffererProfileCommand;
import com.parosurvivors.serviya.profiles.application.ports.input.OffererProfileServicePort;
import com.parosurvivors.serviya.profiles.application.ports.output.OffererProfilePersistencePort;
import com.parosurvivors.serviya.profiles.domain.OffererProfile;
import com.parosurvivors.serviya.profiles.domain.OffererProfileSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de OffererProfileServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class OffererProfileService implements OffererProfileServicePort {

    private final OffererProfilePersistencePort offererProfilePersistencePort;

    @Override
    public OffererProfile getPublicProfile(Long userId) {
        throw new UnsupportedOperationException("TODO: getPublicProfile — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public OffererProfileSummary getProfileSummary(Long userId) {
        throw new UnsupportedOperationException("TODO: getProfileSummary — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public OffererProfile patchOffererProfile(UpdateOffererProfileCommand command) {
        throw new UnsupportedOperationException("TODO: patchOffererProfile — placeholder, ver estructura-servicios.docx");
    }
}
