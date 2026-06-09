package com.parosurvivors.serviya.metrics.application.ports.input;

import com.parosurvivors.serviya.metrics.domain.ClientMetrics;

/**
 * Puerto de entrada de ClientMetricsService (solo consultas). Devuelve dominio (ClientMetrics);
 * el "main" es el mismo agregado, el resumen lo arma el WebMapper. Nunca tipos web.
 * Los métodos de actualización (update/decrement) son @EventListener (no operaciones del puerto).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 6).
 */
public interface ClientMetricsServicePort {

    ClientMetrics getAllMetrics(Long clientId);

    ClientMetrics getMainMetrics(Long clientId);
}
