package com.parosurvivors.serviya.services.infrastructure.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "services")
@Getter
@Setter
public class ServiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offerer_id", nullable = false)
    private Long offererId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "price_hourly", nullable = false)
    private BigDecimal priceHourly;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "average_duration_minutes")
    private Integer averageDurationMinutes;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "operation_radius_km")
    private BigDecimal operationRadiusKm;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
