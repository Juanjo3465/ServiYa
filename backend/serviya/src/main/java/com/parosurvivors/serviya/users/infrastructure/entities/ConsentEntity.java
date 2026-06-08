package com.parosurvivors.serviya.users.infrastructure.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "consents")
@Getter
@Setter
public class ConsentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private Boolean accepted;

    @Column(name = "consented_at")
    private LocalDateTime consentedAt;
}
