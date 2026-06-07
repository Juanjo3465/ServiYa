package com.parosurvivors.serviya.metrics.application.ports.input;

import com.parosurvivors.serviya.metrics.application.dto.ClientMetricsSummaryResponse;
import com.parosurvivors.serviya.metrics.domain.ClientMetrics;

/**
 * Puerto de entrada de ClientMetricsService (solo consultas).
 * Los métodos de actualización (update/decrement) del documento son @EventListener (no operaciones del puerto).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 6).
 */
public interface ClientMetricsServicePort {

    ClientMetrics getAllMetrics(Long clientId);

    ClientMetricsSummaryResponse getMainMetrics(Long clientId);
}
