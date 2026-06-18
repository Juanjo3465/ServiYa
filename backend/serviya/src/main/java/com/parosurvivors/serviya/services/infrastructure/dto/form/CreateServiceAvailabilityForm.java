package com.parosurvivors.serviya.services.infrastructure.dto.form;

import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear la disponibilidad de un servicio")
public record CreateServiceAvailabilityForm (

    @NotBlank @Size(min = 0, max = 6) byte weekDay,
    @NotBlank LocalTime startTime,
    @NotBlank LocalTime endTime,
    @NotBlank boolean isActive

){}
