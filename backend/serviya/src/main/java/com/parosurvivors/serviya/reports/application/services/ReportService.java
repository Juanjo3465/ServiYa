package com.parosurvivors.serviya.reports.application.services;

import com.parosurvivors.serviya.feedback.application.ports.input.ClientFeedbackServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.ServiceFeedbackServicePort;
import com.parosurvivors.serviya.profiles.application.ports.input.UserProfileServicePort;
import com.parosurvivors.serviya.profiles.domain.UserProfile;
import com.parosurvivors.serviya.reports.application.dto.result.FeedbackReportDetail;
import com.parosurvivors.serviya.reports.application.dto.result.PartySummary;
import com.parosurvivors.serviya.reports.application.dto.result.ReportDetailResult;
import com.parosurvivors.serviya.reports.application.dto.result.RequestReportDetail;
import com.parosurvivors.serviya.reports.application.ports.input.ReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.output.ClientFeedbackReportPersistencePort;
import com.parosurvivors.serviya.reports.application.ports.output.ReportPersistencePort;
import com.parosurvivors.serviya.reports.application.ports.output.RequestReportPersistencePort;
import com.parosurvivors.serviya.reports.application.ports.output.ServiceFeedbackReportPersistencePort;
import com.parosurvivors.serviya.reports.domain.Report;
import com.parosurvivors.serviya.reports.domain.ReportPriority;
import com.parosurvivors.serviya.reports.domain.ReportStatus;
import com.parosurvivors.serviya.reports.domain.ReportType;
import com.parosurvivors.serviya.requests.application.dto.result.AdminRequestDetailResult;
import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestQueryServicePort;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportService implements ReportServicePort {

    private final ReportPersistencePort reportPersistencePort;
    private final RequestReportPersistencePort requestReportPersistencePort;
    private final ServiceFeedbackReportPersistencePort serviceFeedbackReportPersistencePort;
    private final ClientFeedbackReportPersistencePort clientFeedbackReportPersistencePort;
    // Puertos de lectura de otros módulos para enriquecer el detalle (composición solo-lectura).
    private final UserProfileServicePort userProfileServicePort;
    private final ServiceRequestQueryServicePort serviceRequestQueryServicePort;
    private final ServiceFeedbackServicePort serviceFeedbackServicePort;
    private final ClientFeedbackServicePort clientFeedbackServicePort;

    @Override
    public Report createBaseReport(Long reporterId, Long reportedUserId, String type, String category, String reason) {
        Report report = Report.builder()
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .reportType(ReportType.valueOf(type))
                .category(category)
                .reason(reason)
                .status(ReportStatus.PENDING)
                .priority(ReportPriority.MEDIUM)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return reportPersistencePort.save(report);
    }

    @Override
    public ReportDetailResult getReportDetail(Long reportId) {
        Report report = reportPersistencePort.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado: " + reportId));

        PartySummary reporter = toParty(report.getReporterId());
        PartySummary reported = toParty(report.getReportedUserId());

        // Despacho por tipo: solo el payload del subtipo correspondiente viene no-nulo; el resto queda null.
        RequestReportDetail request = null;
        FeedbackReportDetail feedback = null;
        switch (report.getReportType()) {
            case REQUEST -> request = requestReportPersistencePort.findByReportId(reportId)
                    .map(link -> buildRequestDetail(link.getRequestId()))
                    .orElse(null);
            case SERVICE_FEEDBACK -> feedback = serviceFeedbackReportPersistencePort.findByReportId(reportId)
                    .map(link -> buildServiceFeedbackDetail(link.getFeedbackId()))
                    .orElse(null);
            case CLIENT_FEEDBACK -> feedback = clientFeedbackReportPersistencePort.findByReportId(reportId)
                    .map(link -> buildClientFeedbackDetail(link.getFeedbackId()))
                    .orElse(null);
        }

        return new ReportDetailResult(
                report.getId(),
                report.getReporterId(),
                report.getReportedUserId(),
                report.getReportType().name(),
                report.getCategory(),
                report.getReason(),
                report.getStatus().name(),
                report.getPriority().name(),
                report.getCreatedAt(),
                report.getUpdatedAt(),
                reporter,
                reported,
                request,
                feedback);
    }

    /** Resumen (nombre + foto) de una parte; si el usuario no tiene perfil, solo el id. */
    private PartySummary toParty(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            UserProfile profile = userProfileServicePort.getProfileInfo(userId);
            return new PartySummary(userId, profile.getFullName(), profile.getProfilePhotoUrl());
        } catch (ResourceNotFoundException e) {
            return new PartySummary(userId, null, null);
        }
    }

    /** Datos básicos de la solicitud reportada; si ya no existe, solo el id. */
    private RequestReportDetail buildRequestDetail(Long requestId) {
        if (requestId == null) {
            return null;
        }
        try {
            AdminRequestDetailResult r = serviceRequestQueryServicePort.getRequestDetailForAdmin(requestId);
            return new RequestReportDetail(
                    r.id(),
                    r.serviceTitle(),
                    r.scheduledDate(),
                    r.status() != null ? r.status().name() : null,
                    r.requestedPrice(),
                    r.city());
        } catch (ResourceNotFoundException e) {
            return new RequestReportDetail(requestId, null, null, null, null, null);
        }
    }

    /** Contenido del feedback de servicio reportado; si fue revertido (borrado), solo id + kind. */
    private FeedbackReportDetail buildServiceFeedbackDetail(Long feedbackId) {
        return serviceFeedbackServicePort.getServiceFeedbackById(feedbackId)
                .map(f -> new FeedbackReportDetail(feedbackId, "SERVICE", f.rating(), f.comment(), f.tags(), f.createdAt()))
                .orElseGet(() -> new FeedbackReportDetail(feedbackId, "SERVICE", null, null, List.of(), null));
    }

    /** Contenido del feedback de cliente reportado; si fue revertido (borrado), solo id + kind. */
    private FeedbackReportDetail buildClientFeedbackDetail(Long feedbackId) {
        return clientFeedbackServicePort.getClientFeedbackById(feedbackId)
                .map(f -> new FeedbackReportDetail(feedbackId, "CLIENT", f.rating(), f.comment(), f.tags(), f.createdAt()))
                .orElseGet(() -> new FeedbackReportDetail(feedbackId, "CLIENT", null, null, List.of(), null));
    }

    @Override
    public Page<Report> getReports(String type, String category, String status, Pageable pageable) {
        List<Report> reports = reportPersistencePort.findAll().stream()
                .filter(report -> type == null || type.isBlank() || report.getReportType().name().equalsIgnoreCase(type))
                .filter(report -> category == null || category.isBlank() || report.getCategory().equalsIgnoreCase(category))
                .filter(report -> status == null || status.isBlank() || report.getStatus().name().equalsIgnoreCase(status))
                .sorted(Comparator.comparing(Report::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), reports.size());
        List<Report> pageContent = start >= reports.size() ? List.of() : reports.subList(start, end);
        return new PageImpl<>(pageContent, pageable, reports.size());
    }

    @Override
    public List<Report> getReportsByReporter(Long reporterId) {
        return reportPersistencePort.findByReporterId(reporterId);
    }

    @Override
    public List<Report> getReportsByReportedUser(Long reportedUserId) {
        return reportPersistencePort.findByReportedUserId(reportedUserId);
    }

    @Override
    public int countReportsByReportedUser(Long reportedUserId) {
        return Math.toIntExact(reportPersistencePort.countByReportedUserId(reportedUserId));
    }

    @Override
    public int countReportsByReporter(Long reporterId) {
        return Math.toIntExact(reportPersistencePort.countByReporterId(reporterId));
    }
}
