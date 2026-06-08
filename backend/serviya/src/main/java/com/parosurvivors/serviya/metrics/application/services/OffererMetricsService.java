package com.parosurvivors.serviya.metrics.application.services;

import com.parosurvivors.serviya.metrics.application.dto.OffererMetricsSummaryResponse;
import com.parosurvivors.serviya.metrics.application.ports.input.OffererMetricsServicePort;
import com.parosurvivors.serviya.metrics.application.ports.output.OffererMetricsPersistencePort;
import com.parosurvivors.serviya.metrics.domain.OffererMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de OffererMetricsServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class OffererMetricsService implements OffererMetricsServicePort {

    private final OffererMetricsPersistencePort offererMetricsPersistencePort;

    @Override
    public OffererMetrics getAllMetrics(Long offererId) {
        throw new UnsupportedOperationException("TODO: getAllMetrics — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public OffererMetricsSummaryResponse getMainMetrics(Long offererId) {
        throw new UnsupportedOperationException("TODO: getMainMetrics — placeholder, ver estructura-servicios.docx");
    }
}
