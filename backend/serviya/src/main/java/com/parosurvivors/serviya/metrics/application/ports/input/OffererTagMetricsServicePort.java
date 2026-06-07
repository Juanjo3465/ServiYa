package com.parosurvivors.serviya.metrics.application.ports.input;

import com.parosurvivors.serviya.metrics.domain.OffererTagMetrics;

import java.util.List;

/**
 * Puerto de entrada de OffererTagMetricsService (solo consultas).
 * Los métodos de actualización (update/decrement) del documento son @EventListener (no operaciones del puerto).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 6).
 */
public interface OffererTagMetricsServicePort {

    List<OffererTagMetrics> getTagMetrics(int offererId);
}
