package com.parosurvivors.serviya.users.application.services;

import com.parosurvivors.serviya.users.application.dto.TokenValidationResult;
import com.parosurvivors.serviya.users.application.ports.input.PasswordResetTokenServicePort;
import com.parosurvivors.serviya.users.application.ports.output.PasswordResetTokenPersistencePort;
import com.parosurvivors.serviya.users.domain.PasswordResetToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de PasswordResetTokenServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class PasswordResetTokenService implements PasswordResetTokenServicePort {

    private final PasswordResetTokenPersistencePort passwordResetTokenPersistencePort;

    @Override
    public PasswordResetToken createToken(Long userId) {
        throw new UnsupportedOperationException("TODO: createToken — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public TokenValidationResult validateToken(Long userId, String rawToken) {
        throw new UnsupportedOperationException("TODO: validateToken — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void markTokenAsUsed(Long tokenId) {
        throw new UnsupportedOperationException("TODO: markTokenAsUsed — placeholder, ver estructura-servicios.docx");
    }
}
