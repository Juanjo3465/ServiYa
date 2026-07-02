package com.parosurvivors.serviya.reports.application.services;

import com.parosurvivors.serviya.reports.application.dto.result.ReportDetailResult;
import com.parosurvivors.serviya.reports.application.ports.input.ReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.output.ClientFeedbackReportPersistencePort;
import com.parosurvivors.serviya.reports.application.ports.output.ReportPersistencePort;
import com.parosurvivors.serviya.reports.application.ports.output.ServiceFeedbackReportPersistencePort;
import com.parosurvivors.serviya.reports.domain.Report;
import com.parosurvivors.serviya.reports.domain.ReportType;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportService implements ReportServicePort {

    private final ReportPersistencePort reportPersistencePort;
    private final ServiceFeedbackReportPersistencePort serviceFeedbackReportPersistencePort;
    private final ClientFeedbackReportPersistencePort clientFeedbackReportPersistencePort;

    @Override
    @Transactional
    public Report createBaseReport(Long reporterId, Long reportedUserId, String type, String category, String reason) {
        Report report = Report.builder()
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .reportType(ReportType.valueOf(type))
                .category(category)
                .reason(reason)
                .createdAt(LocalDateTime.now())
                .build();
        return reportPersistencePort.save(report);
    }

    @Override
    public ReportDetailResult getReportDetail(Long reportId) {
        Report report = reportPersistencePort.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));

        Long requestId = null;
        Long serviceFeedbackId = null;
        Long clientFeedbackId = null;

        if (report.getReportType() == ReportType.SERVICE_FEEDBACK) {
            serviceFeedbackId = serviceFeedbackReportPersistencePort.findByReportId(reportId)
                    .map(ext -> ext.getFeedbackId())
                    .orElse(null);
        } else if (report.getReportType() == ReportType.CLIENT_FEEDBACK) {
            clientFeedbackId = clientFeedbackReportPersistencePort.findByReportId(reportId)
                    .map(ext -> ext.getFeedbackId())
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
                requestId,
                serviceFeedbackId,
                clientFeedbackId);
    }

    @Override
    public Page<Report> getReports(String type, String category, String status, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: getReports — placeholder, ver estructura-servicios.docx");
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
        return (int) reportPersistencePort.countByReportedUserId(reportedUserId);
    }

    @Override
    public int countReportsByReporter(Long reporterId) {
        return (int) reportPersistencePort.countByReporterId(reporterId);
    }
}
