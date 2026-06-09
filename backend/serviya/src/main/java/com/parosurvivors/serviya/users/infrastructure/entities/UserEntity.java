package com.parosurvivors.serviya.users.infrastructure.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "is_banned", nullable = false)
    private Boolean banned;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
