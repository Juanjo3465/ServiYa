package com.parosurvivors.serviya.users.infrastructure.repositories;

import com.parosurvivors.serviya.users.infrastructure.entities.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {
    List<PasswordResetTokenEntity> findByUserId(Long userId);
}
