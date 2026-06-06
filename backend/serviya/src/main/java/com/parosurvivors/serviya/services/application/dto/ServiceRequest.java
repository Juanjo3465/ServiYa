package com.parosurvivors.serviya.services.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequest {
    
    @NotNull(message = "offererId es requerido")
    @Positive(message = "offererId debe ser positivo")
    private Long offererId;
    
    @NotBlank(message = "title es requerido")
    @Size(min = 3, max = 255, message = "title debe tener entre 3 y 255 caracteres")
    private String title;
    
    @NotBlank(message = "description es requerido")
    @Size(min = 10, max = 5000, message = "description debe tener entre 10 y 5000 caracteres")
    private String description;
    
    private List<String> photos;
    
    @NotNull(message = "priceHourly es requerido")
    @DecimalMin(value = "0.01", message = "priceHourly debe ser mayor a 0.01")
    @DecimalMax(value = "999999.99", message = "priceHourly no puede ser mayor a 999999.99")
    private BigDecimal priceHourly;
    
    @NotNull(message = "categoryId es requerido")
    @Positive(message = "categoryId debe ser positivo")
    private Long categoryId;
    
    @Positive(message = "averageDurationMinutes debe ser positivo")
    private Integer averageDurationMinutes;
    
    @DecimalMin(value = "0", message = "operationRadiusKm no puede ser negativo")
    @DecimalMax(value = "500", message = "operationRadiusKm no puede ser mayor a 500 km")
    private BigDecimal operationRadiusKm;
}
