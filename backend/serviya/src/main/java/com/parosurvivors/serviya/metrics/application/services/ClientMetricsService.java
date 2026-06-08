package com.parosurvivors.serviya.metrics.application.services;

import com.parosurvivors.serviya.metrics.application.dto.ClientMetricsSummaryResponse;
import com.parosurvivors.serviya.metrics.application.ports.input.ClientMetricsServicePort;
import com.parosurvivors.serviya.metrics.application.ports.output.ClientMetricsPersistencePort;
import com.parosurvivors.serviya.metrics.domain.ClientMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ClientMetricsServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ClientMetricsService implements ClientMetricsServicePort {

    private final ClientMetricsPersistencePort clientMetricsPersistencePort;

    @Override
    public ClientMetrics getAllMetrics(Long clientId) {
        throw new UnsupportedOperationException("TODO: getAllMetrics — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public ClientMetricsSummaryResponse getMainMetrics(Long clientId) {
        throw new UnsupportedOperationException("TODO: getMainMetrics — placeholder, ver estructura-servicios.docx");
    }
}
