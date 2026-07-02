package com.parosurvivors.serviya.metrics.application.services;

import com.parosurvivors.serviya.metrics.application.ports.input.ServiceTagMetricsServicePort;
import com.parosurvivors.serviya.metrics.application.ports.output.ServiceTagMetricsPersistencePort;
import com.parosurvivors.serviya.metrics.domain.ServiceTagMetrics;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ServiceTagMetricsServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ServiceTagMetricsService implements ServiceTagMetricsServicePort {

    private final ServiceTagMetricsPersistencePort serviceTagMetricsPersistencePort;

    @Override
    public List<ServiceTagMetrics> getTagMetrics(Long serviceId) {
        return serviceTagMetricsPersistencePort.findByServiceId(serviceId);
    }
}
