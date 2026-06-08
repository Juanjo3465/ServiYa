package com.parosurvivors.serviya.reports.infrastructure.adapters.input.api;

import com.parosurvivors.serviya.reports.application.dto.ReportDetailResponse;
import com.parosurvivors.serviya.reports.application.dto.ReportResponse;
import com.parosurvivors.serviya.reports.domain.ClientReviewReport;
import com.parosurvivors.serviya.reports.domain.ReportAction;
import com.parosurvivors.serviya.reports.domain.RequestReport;
import com.parosurvivors.serviya.reports.domain.ServiceReviewReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * Documentacion OpenAPI/Swagger de reportes: creacion por subtipo y consultas (modulo 7, seccion 15).
 * Ver estructura-endpoints.md.
 */
@Tag(name = "Reportes", description = "Creacion de reportes y consultas (consulta solo ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public interface ReportApi {

    @Operation(summary = "Crear un reporte sobre una solicitud", description = "RF-055, RF-057, RF-073.")
    @ApiResponse(responseCode = "201", description = "Reporte de solicitud creado")
    ResponseEntity<RequestReport> createRequestReport(
            @Parameter(description = "Cuerpo con 'reportedUserId', 'category', 'reason', 'requestId'") Map<String, String> body);

    @Operation(summary = "Crear un reporte sobre una resena de servicio", description = "RF-056.")
    @ApiResponse(responseCode = "201", description = "Reporte de resena de servicio creado")
    ResponseEntity<ServiceReviewReport> createServiceReviewReport(
            @Parameter(description = "Cuerpo con 'reportedUserId', 'category', 'reason', 'serviceReviewId'") Map<String, String> body);

    @Operation(summary = "Crear un reporte sobre una resena de cliente", description = "RF-056.")
    @ApiResponse(responseCode = "201", description = "Reporte de resena de cliente creado")
    ResponseEntity<ClientReviewReport> createClientReviewReport(
            @Parameter(description = "Cuerpo con 'reportedUserId', 'category', 'reason', 'clientReviewId'") Map<String, String> body);

    @Operation(summary = "Listar reportes para el panel admin", description = "RF-058.")
    @ApiResponse(responseCode = "200", description = "Pagina de reportes")
    ResponseEntity<Page<ReportResponse>> getReports(
            @Parameter(description = "Tipo: REQUEST, SERVICE_REVIEW, CLIENT_REVIEW (opcional)") String type,
            @Parameter(description = "Categoria (opcional)") String category,
            @Parameter(description = "Estado (opcional)") String status, Pageable pageable);

    @Operation(summary = "Detalle de un reporte (dispatch por tipo)", description = "RF-058.")
    @ApiResponse(responseCode = "200", description = "Detalle base del reporte")
    ResponseEntity<ReportDetailResponse> getReportDetail(@Parameter(description = "Id del reporte") Long id);

    @Operation(summary = "Historial de acciones de un reporte", description = "RF-071.")
    @ApiResponse(responseCode = "200", description = "Acciones del reporte")
    ResponseEntity<List<ReportAction>> getReportActions(@Parameter(description = "Id del reporte") Long id);

    @Operation(summary = "Reportes recibidos por un usuario", description = "RF-081.")
    @ApiResponse(responseCode = "200", description = "Reportes recibidos")
    ResponseEntity<List<ReportResponse>> getReportsReceived(@Parameter(description = "Id del usuario") Long id);

    @Operation(summary = "Reportes enviados por un usuario", description = "RF-081.")
    @ApiResponse(responseCode = "200", description = "Reportes enviados")
    ResponseEntity<List<ReportResponse>> getReportsSent(@Parameter(description = "Id del usuario") Long id);
}
