package com.parosurvivors.serviya.profiles.infrastructure.entities;

import com.parosurvivors.serviya.profiles.domain.ProfileType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
public class UserProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    /** Cifrado AES-256-GCM. Pendiente el AttributeConverter de cifrado (ver NOTAS.txt). */
    @Column(name = "document_number", nullable = false, columnDefinition = "VARBINARY(512)")
    private byte[] documentNumber;

    /** Cifrado AES-256-GCM. Pendiente el AttributeConverter de cifrado (ver NOTAS.txt). */
    @Column(name = "phone_number", nullable = false, columnDefinition = "VARBINARY(512)")
    private byte[] phoneNumber;

    @Column(name = "primary_address_id")
    private Long primaryAddressId;

    @Column(name = "profile_photo_url", length = 1000)
    private String profilePhotoUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_type", nullable = false)
    private ProfileType profileType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
