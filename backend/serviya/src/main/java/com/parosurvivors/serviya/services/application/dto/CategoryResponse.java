package com.parosurvivors.serviya.services.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta de categoría en el marketplace")
public class CategoryResponse {
    
    @Schema(description = "ID único de la categoría", example = "1")
    private Long id;
    
    @Schema(description = "Nombre de la categoría", example = "Reparación")
    private String name;
}
