package com.parosurvivors.serviya.services.application.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * DTO de búsqueda para servicios. Es inmutable y dispone de un `@Builder` para construcción opcional.
 */
@Builder
public record SearchServiceQuery(
        // Full text / name search over title and description
        String name,

        // Category filter
        Long categoryId,

        // Exact offerer id (oferente)
        Long offererId,

        // Price range (hourly)
        BigDecimal minPrice,
        BigDecimal maxPrice,

        // Availability / active flag
        Boolean available,

        // Optional: rating, offerer type, proximity, etc. — these may require external joins
        Double minRating,
        Double maxRating,
        String offererType,

        // Location-based filters (latitude/longitude + max distance in km)
        Double latitude,
        Double longitude,
        Double maxDistanceKm
) {
}
