package com.parosurvivors.serviya.metrics.application.ports.input;

import com.parosurvivors.serviya.metrics.application.dto.OffererMetricsSummaryResponse;
import com.parosurvivors.serviya.metrics.domain.OffererMetrics;

/**
 * Puerto de entrada de OffererMetricsService (solo consultas).
 * Los métodos de actualización (update/decrement) del documento son @EventListener (no operaciones del puerto).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 6).
 */
public interface OffererMetricsServicePort {

    OffererMetrics getAllMetrics(Long offererId);

    OffererMetricsSummaryResponse getMainMetrics(Long offererId);
}
