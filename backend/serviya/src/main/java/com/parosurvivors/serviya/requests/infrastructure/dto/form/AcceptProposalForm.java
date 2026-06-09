package com.parosurvivors.serviya.requests.infrastructure.dto.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Entrada web (Form) para aceptar una propuesta de reprogramacion (cliente).
 * POST /api/v1/reschedule-proposals/{id}/accept (RF-035). El clientId se extrae del JWT.
 * TODO: revisar validaciones.
 */
@Schema(description = "Fecha confirmada al aceptar la propuesta")
public record AcceptProposalForm(
        @NotNull @Future LocalDateTime confirmedDate) {
}
