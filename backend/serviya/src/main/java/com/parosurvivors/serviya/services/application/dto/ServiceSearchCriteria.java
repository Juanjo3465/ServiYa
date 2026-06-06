package com.parosurvivors.serviya.services.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSearchCriteria {
    // Full text / name search over title and description
    private String name;

    // Category filter
    private Long categoryId;

    // Exact offerer id (oferente)
    private Long offererId;

    // Price range (hourly)
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // Availability / active flag
    private Boolean available;

    // Optional: rating, offerer type, proximity, etc. — these may require external joins
    private Double minRating;
    private Double maxRating;
    private String offererType;

    // Location-based filters (latitude/longitude + max distance in km)
    private Double latitude;
    private Double longitude;
    private Double maxDistanceKm;
}
