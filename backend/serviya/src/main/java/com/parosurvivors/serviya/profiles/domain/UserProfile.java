package com.parosurvivors.serviya.profiles.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Información personal del usuario. Mapea la tabla {@code user_profiles} (1:1 con users).
 * {@code documentNumber} y {@code phoneNumber} se guardan cifrados (AES-256-GCM);
 * en el dominio se manejan en claro (ver CLAUDE.md, "Security/PII").
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    private Long id;
    private Long userId;
    private String fullName;
    private String documentType;
    private String documentNumber;
    private String phoneNumber;
    private Long primaryAddressId;
    private String profilePhotoUrl;
    private String bio;
    private ProfileType profileType;
    private LocalDateTime createdAt;

    // =====================================================
    // BUSINESS METHODS
    // =====================================================

    public void updateMainAddress(Long addressId) {
        this.primaryAddressId = addressId;
    }

    public boolean hasMainAddress() {
        return primaryAddressId != null;
    }
}
