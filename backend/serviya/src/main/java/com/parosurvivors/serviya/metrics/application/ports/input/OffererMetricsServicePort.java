package com.parosurvivors.serviya.metrics.application.ports.input;

import com.parosurvivors.serviya.metrics.domain.OffererMetrics;

/**
 * Puerto de entrada de OffererMetricsService (solo consultas). Devuelve dominio (OffererMetrics);
 * el "main" es el mismo agregado, el resumen lo arma el WebMapper. Nunca tipos web.
 * Los métodos de actualización (update/decrement) son @EventListener (no operaciones del puerto).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 6).
 */
public interface OffererMetricsServicePort {

    OffererMetrics getAllMetrics(Long offererId);

    OffererMetrics getMainMetrics(Long offererId);
}
