package com.parosurvivors.serviya.reports.infrastructure.adapters.input;

import com.parosurvivors.serviya.reports.application.dto.ReportDetailResponse;
import com.parosurvivors.serviya.reports.application.dto.ReportResponse;
import com.parosurvivors.serviya.reports.application.ports.input.ClientReviewReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.ReportActionServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.ReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.RequestReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.ServiceReviewReportServicePort;
import com.parosurvivors.serviya.reports.domain.ClientReviewReport;
import com.parosurvivors.serviya.reports.domain.ReportAction;
import com.parosurvivors.serviya.reports.domain.RequestReport;
import com.parosurvivors.serviya.reports.domain.ServiceReviewReport;
import com.parosurvivors.serviya.reports.infrastructure.adapters.input.api.ReportApi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Adaptador de entrada (REST) de reportes. Placeholder funcional; documentacion en {@link ReportApi}.
 */
@RestController
@RequiredArgsConstructor
public class ReportController implements ReportApi {

    private final ReportServicePort reportService;
    private final RequestReportServicePort requestReportService;
    private final ServiceReviewReportServicePort serviceReviewReportService;
    private final ClientReviewReportServicePort clientReviewReportService;
    private final ReportActionServicePort reportActionService;

    @Override
    @PostMapping("/api/v1/reports/requests")
    public ResponseEntity<RequestReport> createRequestReport(@RequestBody Map<String, String> body) {
        RequestReport created = requestReportService.createReport(
                currentUserId(), Long.valueOf(body.get("reportedUserId")),
                body.get("category"), body.get("reason"), Long.valueOf(body.get("requestId")));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    @PostMapping("/api/v1/reports/service-reviews")
    public ResponseEntity<ServiceReviewReport> createServiceReviewReport(@RequestBody Map<String, String> body) {
        ServiceReviewReport created = serviceReviewReportService.createReport(
                currentUserId(), Long.valueOf(body.get("reportedUserId")),
                body.get("category"), body.get("reason"), Long.valueOf(body.get("serviceReviewId")));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    @PostMapping("/api/v1/reports/client-reviews")
    public ResponseEntity<ClientReviewReport> createClientReviewReport(@RequestBody Map<String, String> body) {
        ClientReviewReport created = clientReviewReportService.createReport(
                currentUserId(), Long.valueOf(body.get("reportedUserId")),
                body.get("category"), body.get("reason"), Long.valueOf(body.get("clientReviewId")));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    @GetMapping("/api/v1/reports")
    public ResponseEntity<Page<ReportResponse>> getReports(@RequestParam(required = false) String type,
                                                          @RequestParam(required = false) String category,
                                                          @RequestParam(required = false) String status,
                                                          Pageable pageable) {
        return ResponseEntity.ok(reportService.getReports(type, category, status, pageable));
    }

    @Override
    @GetMapping("/api/v1/reports/{id}")
    public ResponseEntity<ReportDetailResponse> getReportDetail(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getReportDetail(id));
    }

    @Override
    @GetMapping("/api/v1/reports/{id}/actions")
    public ResponseEntity<List<ReportAction>> getReportActions(@PathVariable Long id) {
        return ResponseEntity.ok(reportActionService.getActionsByReport(id));
    }

    @Override
    @GetMapping("/api/v1/users/{id}/reports/received")
    public ResponseEntity<List<ReportResponse>> getReportsReceived(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getReportsByReportedUser(id));
    }

    @Override
    @GetMapping("/api/v1/users/{id}/reports/sent")
    public ResponseEntity<List<ReportResponse>> getReportsSent(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getReportsByReporter(id));
    }

    /** TODO: reemplazar por el id extraido del JWT autenticado. */
    private Long currentUserId() {
        return 0L;
    }
}
