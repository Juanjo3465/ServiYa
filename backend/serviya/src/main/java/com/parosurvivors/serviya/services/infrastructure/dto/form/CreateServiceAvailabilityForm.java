package com.parosurvivors.serviya.services.infrastructure.dto.form;

import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear la disponibilidad de un servicio")
public record CreateServiceAvailabilityForm (
    @NotNull Integer weekDay,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    @Schema(description = "Si está activo. Por defecto true si no se proporciona.")
    Boolean isActive
){}
