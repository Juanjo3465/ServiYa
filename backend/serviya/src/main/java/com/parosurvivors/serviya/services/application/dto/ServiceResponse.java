package com.parosurvivors.serviya.services.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response de un servicio")
public class ServiceResponse {
    
    @Schema(description = "ID único del servicio", example = "1")
    private Long id;
    
    @Schema(description = "ID del oferente", example = "5")
    private Long offererId;
    
    @Schema(description = "Título del servicio", example = "Reparación de computadoras")
    private String title;
    
    @Schema(description = "Descripción detallada", example = "Reparación de hardware y software")
    private String description;
    
    @Schema(description = "URLs de fotos del servicio")
    private List<String> photos;
    
    @Schema(description = "Precio por hora", example = "50.00")
    private BigDecimal priceHourly;
    
    @Schema(description = "Categoría ID", example = "2")
    private Long categoryId;
    
    @Schema(description = "Duración promedio en minutos", example = "30")
    private Integer averageDurationMinutes;
    
    @Schema(description = "¿Está activo?", example = "true")
    private Boolean active;
    
    @Schema(description = "Radio de operación en km", example = "15.5")
    private BigDecimal operationRadiusKm;
    
    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;
    
    @Schema(description = "Fecha de última actualización")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Fecha de eliminación (si está eliminado)")
    private LocalDateTime deletedAt;
}
