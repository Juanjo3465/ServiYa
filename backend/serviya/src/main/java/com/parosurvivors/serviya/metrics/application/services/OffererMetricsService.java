package com.parosurvivors.serviya.metrics.application.services;

import com.parosurvivors.serviya.metrics.application.ports.input.OffererMetricsServicePort;
import com.parosurvivors.serviya.metrics.application.ports.output.OffererMetricsPersistencePort;
import com.parosurvivors.serviya.metrics.domain.OffererMetrics;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OffererMetricsService implements OffererMetricsServicePort {

    private final OffererMetricsPersistencePort offererMetricsPersistencePort;

    @Override
    public OffererMetrics getAllMetrics(Long offererId) {
        return offererMetricsPersistencePort.findByOffererId(offererId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Offerer metrics not found for offererId: " + offererId));
    }

    @Override
    public OffererMetrics getMainMetrics(Long offererId) {
        return offererMetricsPersistencePort.findByOffererId(offererId)
                .orElse(OffererMetrics.builder().offererId(offererId).build());
    }
}
