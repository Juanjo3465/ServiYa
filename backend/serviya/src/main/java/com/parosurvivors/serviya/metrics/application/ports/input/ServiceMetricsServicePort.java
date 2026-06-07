package com.parosurvivors.serviya.metrics.application.ports.input;

import com.parosurvivors.serviya.metrics.domain.ServiceMetrics;

/**
 * Puerto de entrada de ServiceMetricsService (solo consultas).
 * Los métodos de actualización (update/decrement) del documento son @EventListener (no operaciones del puerto).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 6).
 */
public interface ServiceMetricsServicePort {

    ServiceMetrics getMetrics(int serviceId);
}
