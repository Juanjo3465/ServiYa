package com.parosurvivors.serviya.reports.application.ports.input;

import com.parosurvivors.serviya.reports.application.dto.ReportDetailResponse;
import com.parosurvivors.serviya.reports.application.dto.ReportResponse;
import com.parosurvivors.serviya.reports.domain.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Puerto de entrada de ReportService — consultas y lógica base compartida (por composición).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 7).
 */
public interface ReportServicePort {

    Report createBaseReport(Long reporterId, Long reportedUserId, String type, String category, String reason);

    ReportDetailResponse getReportDetail(Long reportId);

    Page<ReportResponse> getReports(String type, String category, String status, Pageable pageable);

    List<ReportResponse> getReportsByReporter(Long reporterId);

    List<ReportResponse> getReportsByReportedUser(Long reportedUserId);

    int countReportsByReportedUser(Long reportedUserId);

    int countReportsByReporter(Long reporterId);
}
