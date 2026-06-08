package com.parosurvivors.serviya.metrics.infrastructure.adapters.input;

import com.parosurvivors.serviya.metrics.application.dto.ClientMetricsSummaryResponse;
import com.parosurvivors.serviya.metrics.application.dto.OffererMetricsSummaryResponse;
import com.parosurvivors.serviya.metrics.application.ports.input.ClientMetricsServicePort;
import com.parosurvivors.serviya.metrics.application.ports.input.ClientTagMetricsServicePort;
import com.parosurvivors.serviya.metrics.application.ports.input.OffererMetricsServicePort;
import com.parosurvivors.serviya.metrics.application.ports.input.OffererTagMetricsServicePort;
import com.parosurvivors.serviya.metrics.application.ports.input.ServiceMetricsServicePort;
import com.parosurvivors.serviya.metrics.application.ports.input.ServiceTagMetricsServicePort;
import com.parosurvivors.serviya.metrics.domain.ClientMetrics;
import com.parosurvivors.serviya.metrics.domain.ClientTagMetrics;
import com.parosurvivors.serviya.metrics.domain.OffererMetrics;
import com.parosurvivors.serviya.metrics.domain.OffererTagMetrics;
import com.parosurvivors.serviya.metrics.domain.ServiceMetrics;
import com.parosurvivors.serviya.metrics.domain.ServiceTagMetrics;
import com.parosurvivors.serviya.metrics.infrastructure.adapters.input.api.MetricsApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptador de entrada (REST) de metricas. Placeholder funcional; documentacion en {@link MetricsApi}.
 */
@RestController
@RequiredArgsConstructor
public class MetricsController implements MetricsApi {

    private final ServiceMetricsServicePort serviceMetricsService;
    private final ServiceTagMetricsServicePort serviceTagMetricsService;
    private final OffererMetricsServicePort offererMetricsService;
    private final OffererTagMetricsServicePort offererTagMetricsService;
    private final ClientMetricsServicePort clientMetricsService;
    private final ClientTagMetricsServicePort clientTagMetricsService;

    @Override
    @GetMapping("/api/v1/services/{id}/metrics")
    public ResponseEntity<ServiceMetrics> getServiceMetrics(@PathVariable Long id) {
        return ResponseEntity.ok(serviceMetricsService.getMetrics(id));
    }

    @Override
    @GetMapping("/api/v1/services/{id}/tag-metrics")
    public ResponseEntity<List<ServiceTagMetrics>> getServiceTagMetrics(@PathVariable Long id) {
        return ResponseEntity.ok(serviceTagMetricsService.getTagMetrics(id));
    }

    @Override
    @GetMapping("/api/v1/offerers/{id}/metrics")
    public ResponseEntity<OffererMetrics> getOffererMetrics(@PathVariable Long id) {
        return ResponseEntity.ok(offererMetricsService.getAllMetrics(id));
    }

    @Override
    @GetMapping("/api/v1/offerers/{id}/metrics/main")
    public ResponseEntity<OffererMetricsSummaryResponse> getOffererMainMetrics(@PathVariable Long id) {
        return ResponseEntity.ok(offererMetricsService.getMainMetrics(id));
    }

    @Override
    @GetMapping("/api/v1/offerers/{id}/tag-metrics")
    public ResponseEntity<List<OffererTagMetrics>> getOffererTagMetrics(@PathVariable Long id) {
        return ResponseEntity.ok(offererTagMetricsService.getTagMetrics(id));
    }

    @Override
    @GetMapping("/api/v1/clients/{id}/metrics")
    public ResponseEntity<ClientMetrics> getClientMetrics(@PathVariable Long id) {
        return ResponseEntity.ok(clientMetricsService.getAllMetrics(id));
    }

    @Override
    @GetMapping("/api/v1/clients/{id}/metrics/main")
    public ResponseEntity<ClientMetricsSummaryResponse> getClientMainMetrics(@PathVariable Long id) {
        return ResponseEntity.ok(clientMetricsService.getMainMetrics(id));
    }

    @Override
    @GetMapping("/api/v1/clients/{id}/tag-metrics")
    public ResponseEntity<List<ClientTagMetrics>> getClientTagMetrics(@PathVariable Long id) {
        return ResponseEntity.ok(clientTagMetricsService.getTagMetrics(id));
    }

    @Override
    @GetMapping("/api/v1/users/me/metrics")
    public ResponseEntity<Map<String, Object>> getOwnMetrics() {
        Long userId = currentUserId();
        Map<String, Object> result = new HashMap<>();
        result.put("offererMetrics", offererMetricsService.getAllMetrics(userId));
        result.put("clientMetrics", clientMetricsService.getAllMetrics(userId));
        return ResponseEntity.ok(result);
    }

    /** TODO: reemplazar por el id extraido del JWT autenticado. */
    private Long currentUserId() {
        return 0L;
    }
}
