package com.parosurvivors.serviya.metrics.application.services;

import com.parosurvivors.serviya.metrics.application.ports.input.ClientTagMetricsServicePort;
import com.parosurvivors.serviya.metrics.application.ports.output.ClientTagMetricsPersistencePort;
import com.parosurvivors.serviya.metrics.domain.ClientTagMetrics;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ClientTagMetricsServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ClientTagMetricsService implements ClientTagMetricsServicePort {

    private final ClientTagMetricsPersistencePort clientTagMetricsPersistencePort;

    @Override
    public List<ClientTagMetrics> getTagMetrics(Long clientId) {
        throw new UnsupportedOperationException("TODO: getTagMetrics — placeholder, ver estructura-servicios.docx");
    }
}
