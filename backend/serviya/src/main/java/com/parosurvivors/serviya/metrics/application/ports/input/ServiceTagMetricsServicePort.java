package com.parosurvivors.serviya.metrics.application.ports.input;

import com.parosurvivors.serviya.metrics.domain.ServiceTagMetrics;

import java.util.List;

/**
 * Puerto de entrada de ServiceTagMetricsService (solo consultas).
 * Los métodos de actualización (update/decrement) del documento son @EventListener (no operaciones del puerto).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 6).
 */
public interface ServiceTagMetricsServicePort {

    List<ServiceTagMetrics> getTagMetrics(Long serviceId);
}
