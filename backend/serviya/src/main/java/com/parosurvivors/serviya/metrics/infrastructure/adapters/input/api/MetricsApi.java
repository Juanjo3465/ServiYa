package com.parosurvivors.serviya.metrics.infrastructure.adapters.input.api;

import com.parosurvivors.serviya.metrics.application.dto.ClientMetricsSummaryResponse;
import com.parosurvivors.serviya.metrics.application.dto.OffererMetricsSummaryResponse;
import com.parosurvivors.serviya.metrics.domain.ClientMetrics;
import com.parosurvivors.serviya.metrics.domain.ClientTagMetrics;
import com.parosurvivors.serviya.metrics.domain.OffererMetrics;
import com.parosurvivors.serviya.metrics.domain.OffererTagMetrics;
import com.parosurvivors.serviya.metrics.domain.ServiceMetrics;
import com.parosurvivors.serviya.metrics.domain.ServiceTagMetrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * Documentacion OpenAPI/Swagger de las metricas precalculadas (modulo 6, seccion 14).
 * Ver estructura-endpoints.md.
 */
@Tag(name = "Metricas", description = "Metricas precalculadas de servicios, oferentes y clientes")
public interface MetricsApi {

    @Operation(summary = "Metricas de un servicio", description = "RF-040.")
    @ApiResponse(responseCode = "200", description = "Metricas del servicio")
    ResponseEntity<ServiceMetrics> getServiceMetrics(@Parameter(description = "Id del servicio") Long id);

    @Operation(summary = "Conteo de tags de un servicio")
    @ApiResponse(responseCode = "200", description = "Tag-metrics del servicio")
    ResponseEntity<List<ServiceTagMetrics>> getServiceTagMetrics(@Parameter(description = "Id del servicio") Long id);

    @Operation(summary = "Todas las metricas de un oferente", description = "RF-042, RF-053.")
    @ApiResponse(responseCode = "200", description = "Metricas del oferente")
    ResponseEntity<OffererMetrics> getOffererMetrics(@Parameter(description = "Id del oferente") Long id);

    @Operation(summary = "Metricas principales (resumen) de un oferente", description = "RF-042, RF-053.")
    @ApiResponse(responseCode = "200", description = "Resumen de metricas del oferente")
    ResponseEntity<OffererMetricsSummaryResponse> getOffererMainMetrics(
            @Parameter(description = "Id del oferente") Long id);

    @Operation(summary = "Conteo de tags de un oferente")
    @ApiResponse(responseCode = "200", description = "Tag-metrics del oferente")
    ResponseEntity<List<OffererTagMetrics>> getOffererTagMetrics(@Parameter(description = "Id del oferente") Long id);

    @Operation(summary = "Todas las metricas de un cliente", description = "RF-054.")
    @ApiResponse(responseCode = "200", description = "Metricas del cliente")
    ResponseEntity<ClientMetrics> getClientMetrics(@Parameter(description = "Id del cliente") Long id);

    @Operation(summary = "Metricas principales (resumen) de un cliente", description = "RF-054.")
    @ApiResponse(responseCode = "200", description = "Resumen de metricas del cliente")
    ResponseEntity<ClientMetricsSummaryResponse> getClientMainMetrics(
            @Parameter(description = "Id del cliente") Long id);

    @Operation(summary = "Conteo de tags de un cliente")
    @ApiResponse(responseCode = "200", description = "Tag-metrics del cliente")
    ResponseEntity<List<ClientTagMetrics>> getClientTagMetrics(@Parameter(description = "Id del cliente") Long id);

    @Operation(summary = "Metricas propias segun los roles del usuario",
            description = "Devuelve offererMetrics y/o clientMetrics. RF-051, RF-052.")
    @ApiResponse(responseCode = "200", description = "Metricas del usuario autenticado")
    ResponseEntity<Map<String, Object>> getOwnMetrics();
}
