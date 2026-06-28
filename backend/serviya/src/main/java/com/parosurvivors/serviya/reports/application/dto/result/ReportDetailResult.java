package com.parosurvivors.serviya.reports.application.dto.result;

import java.time.LocalDateTime;

/**
 * Salida de aplicacion (Result) del detalle de un reporte (CQRS-light). Vista paraguas del dispatch por
 * tipo: trae los campos base del Report + los campos especificos del subtipo (solo uno no-nulo segun reportType).
 * Lo devuelve ReportService.getReportDetail (y los subservicios al despachar). Evita un Result/Response por subtipo.
 * TODO: revisar campos enriquecidos por subtipo.
 */
public record ReportDetailResult(
        Long id,
        Long reporterId,
        Long reportedUserId,
        String reportType,
        String category,
        String reason,
        String status,
        String priority,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        // Campos especificos del subtipo (solo el correspondiente al reportType viene no-nulo)
        Long requestId,
        Long serviceFeedbackId,
        Long clientFeedbackId) {
}
