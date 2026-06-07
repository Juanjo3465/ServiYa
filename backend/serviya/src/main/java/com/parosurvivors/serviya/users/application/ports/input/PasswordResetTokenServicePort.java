package com.parosurvivors.serviya.users.application.ports.input;

import com.parosurvivors.serviya.users.application.dto.TokenValidationResult;
import com.parosurvivors.serviya.users.domain.PasswordResetToken;

/**
 * Puerto de entrada de PasswordResetTokenService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 1).
 */
public interface PasswordResetTokenServicePort {

    PasswordResetToken createToken(int userId);

    TokenValidationResult validateToken(int userId, String rawToken);

    void markTokenAsUsed(int tokenId);
}
