package com.parosurvivors.serviya.users.application.services;

import com.parosurvivors.serviya.users.application.ports.input.ConsentServicePort;
import com.parosurvivors.serviya.users.application.ports.output.ConsentPersistencePort;
import com.parosurvivors.serviya.users.domain.Consent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ConsentServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ConsentService implements ConsentServicePort {

    private final ConsentPersistencePort consentPersistencePort;

    @Override
    public Consent createConsent(Long userId, boolean accepted) {
        throw new UnsupportedOperationException("TODO: createConsent — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public boolean hasConsented(Long userId) {
        throw new UnsupportedOperationException("TODO: hasConsented — placeholder, ver estructura-servicios.docx");
    }
}
