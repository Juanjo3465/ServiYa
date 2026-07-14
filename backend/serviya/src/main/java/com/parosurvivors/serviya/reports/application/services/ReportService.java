package com.parosurvivors.serviya.reports.application.services;

import com.parosurvivors.serviya.reports.application.dto.result.ReportDetailResult;
import com.parosurvivors.serviya.reports.application.ports.input.ReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.output.ReportPersistencePort;
import com.parosurvivors.serviya.reports.domain.Report;
import com.parosurvivors.serviya.reports.domain.ReportPriority;
import com.parosurvivors.serviya.reports.domain.ReportStatus;
import com.parosurvivors.serviya.reports.domain.ReportType;
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
                null,
                null,
                null);
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

    @Override
    public void closeReport(Long reportId) {
        Report report = reportPersistencePort.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado: " + reportId));
        report.close();
        reportPersistencePort.update(report);
    }
}
