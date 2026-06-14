package com.parosurvivors.serviya.services.infrastructure.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Salida web (Response) para detalle de servicio. Se usa cuando se necesita
 * informacion compuesta del servicio + datos publicos del oferente.
 */
@Schema(description = "Detalle de un servicio con informacion del oferente")
public record ServiceDetailResponse(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Long id,
        OffererProfileResponse offerer,
        CategoryResponse category,
        String title,
        String description,
        List<String> photos,
        BigDecimal priceHourly,
        Integer averageDurationMinutes,
        Boolean active,
        BigDecimal operationRadiusKm,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt) {
}
