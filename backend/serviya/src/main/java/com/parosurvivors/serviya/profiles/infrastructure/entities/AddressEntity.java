package com.parosurvivors.serviya.profiles.infrastructure.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "addresses")
@Getter
@Setter
public class AddressEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Cifrado AES-256-GCM. Pendiente el AttributeConverter de cifrado (ver NOTAS.txt). */
    @Column(name = "address_line", nullable = false, columnDefinition = "VARBINARY(1024)")
    private byte[] addressLine;

    @Column(nullable = false, length = 150)
    private String city;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
