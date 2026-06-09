package com.parosurvivors.serviya.metrics.application.services;

import com.parosurvivors.serviya.metrics.application.ports.input.OffererTagMetricsServicePort;
import com.parosurvivors.serviya.metrics.application.ports.output.OffererTagMetricsPersistencePort;
import com.parosurvivors.serviya.metrics.domain.OffererTagMetrics;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de OffererTagMetricsServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class OffererTagMetricsService implements OffererTagMetricsServicePort {

    private final OffererTagMetricsPersistencePort offererTagMetricsPersistencePort;

    @Override
    public List<OffererTagMetrics> getTagMetrics(Long offererId) {
        throw new UnsupportedOperationException("TODO: getTagMetrics — placeholder, ver estructura-servicios.docx");
    }
}
