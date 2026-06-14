package com.parosurvivors.serviya.services.infrastructure.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.List;


@Entity
@Immutable
@Table(name = "view_service_details")
@Getter
public class ServiceDetailEntity {
    @Id
    private Long id;

    @Column(name = "title")
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> photos;

    @Column(name = "price_hourly")
    private BigDecimal priceHourly;

    @Column(name = "average_duration_minutes")
    private Integer averageDurationMinutes;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "operation_radius_km")
    private BigDecimal operationRadiusKm;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "offerer_id")
    private Long offererId;

    @Column(name = "offerer_full_name")
    private String offererFullName;

    @Column(name = "offerer_phone_number")
    private String offererPhoneNumber;

    @Column(name = "offerer_profile_photo_url")
    private String offererProfilePhotoUrl;

    @Column(name = "public_description", columnDefinition = "TEXT")
    private String publicDescription;

    @Column(name = "specialty")
    private String specialty;

    @Column(name = "offerer_bio", columnDefinition = "TEXT")
    private String offererBio;

    @Column(name = "offerer_average_rating")
    private BigDecimal offererAverageRating;

    @Column(name = "offerer_total_completed_services")
    private Integer offererTotalCompletedServices;

    @Column(name = "category_name")
    private String categoryName;
}
