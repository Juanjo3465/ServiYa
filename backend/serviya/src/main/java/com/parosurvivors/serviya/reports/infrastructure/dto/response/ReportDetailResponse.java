package com.parosurvivors.serviya.reports.infrastructure.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Salida web (Response) del detalle de un reporte (paraguas por subtipo). Mapea desde ReportDetailResult.
 * Solo el campo de subtipo correspondiente al reportType viene no-nulo.
 * TODO: revisar campos.
 */
@Schema(description = "Detalle de un reporte (con campos del subtipo)")
public record ReportDetailResponse(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Long id,
        Long reporterId,
        Long reportedUserId,
        String reportType,
        String category,
        String reason,
        String status,
        String priority,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long requestId,
        Long serviceFeedbackId,
        Long clientFeedbackId) {
}
