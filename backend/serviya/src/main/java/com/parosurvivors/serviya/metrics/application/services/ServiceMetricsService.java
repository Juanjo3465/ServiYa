package com.parosurvivors.serviya.metrics.application.services;

import com.parosurvivors.serviya.metrics.application.ports.input.ServiceMetricsServicePort;
import com.parosurvivors.serviya.metrics.application.ports.output.ServiceMetricsPersistencePort;
import com.parosurvivors.serviya.metrics.domain.ServiceMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ServiceMetricsServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ServiceMetricsService implements ServiceMetricsServicePort {

    private final ServiceMetricsPersistencePort serviceMetricsPersistencePort;

    @Override
    public ServiceMetrics getMetrics(Long serviceId) {
        throw new UnsupportedOperationException("TODO: getMetrics — placeholder, ver estructura-servicios.docx");
    }
}
