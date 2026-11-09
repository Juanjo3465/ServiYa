package com.parosurvivors.serviya.services.application.dto.query;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * DTO de búsqueda para servicios. Es inmutable y dispone de un `@Builder` para construcción opcional.
 */
@Builder
public record SearchServiceQuery(
        // Full text / name search over title and description
        String name,
        Long categoryId,
        Long offererId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean available,
        Double minRating,
        Double maxRating,
        Double latitude,
        Double longitude,
        Double maxDistanceKm,
        // Ids resueltos por metrics.api para el filtro de rating; null/vacio = sin filtro.
        java.util.List<Long> serviceIds
) {

    public SearchServiceQuery withServiceIds(java.util.List<Long> ids) {
        return new SearchServiceQuery(
                name, categoryId, offererId, minPrice, maxPrice, available,
                minRating, maxRating, latitude, longitude, maxDistanceKm, ids);
    }
}