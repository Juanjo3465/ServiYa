package com.parosurvivors.serviya.services.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequest {

    @NotNull(message = "name es requerido")
    @Size(min = 3, max = 150, message = "name debe tener entre 3 y 150 caracteres")
    private String name;    
}
