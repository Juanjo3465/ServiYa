package com.parosurvivors.serviya.users.application.ports.output;

import com.parosurvivors.serviya.users.domain.PasswordResetToken;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenPersistencePort {
    PasswordResetToken save(PasswordResetToken token);
    PasswordResetToken update(PasswordResetToken token);
    Optional<PasswordResetToken> findById(Long id);
    List<PasswordResetToken> findByUserId(Long userId);
}
