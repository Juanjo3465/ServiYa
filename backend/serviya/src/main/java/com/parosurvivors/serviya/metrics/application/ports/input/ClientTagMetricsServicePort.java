package com.parosurvivors.serviya.metrics.application.ports.input;

import com.parosurvivors.serviya.metrics.domain.ClientTagMetrics;

import java.util.List;

/**
 * Puerto de entrada de ClientTagMetricsService (solo consultas).
 * Los métodos de actualización (update/decrement) del documento son @EventListener (no operaciones del puerto).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 6).
 */
public interface ClientTagMetricsServicePort {

    List<ClientTagMetrics> getTagMetrics(Long clientId);
}
