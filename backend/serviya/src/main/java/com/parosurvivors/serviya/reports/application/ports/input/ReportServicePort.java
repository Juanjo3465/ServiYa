package com.parosurvivors.serviya.reports.application.ports.input;

import com.parosurvivors.serviya.reports.application.dto.result.ReportDetailResult;
import com.parosurvivors.serviya.reports.domain.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Puerto de entrada de ReportService — consultas y lógica base compartida (por composición).
 * Las lecturas devuelven dominio (Report) o el Result paraguas del detalle. Nunca tipos web.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 7).
 */
public interface ReportServicePort {

    Report createBaseReport(Long reporterId, Long reportedUserId, String type, String category, String reason);

    ReportDetailResult getReportDetail(Long reportId);

    Page<Report> getReports(String type, String category, String status, Pageable pageable);

    List<Report> getReportsByReporter(Long reporterId);

    List<Report> getReportsByReportedUser(Long reportedUserId);

    int countReportsByReportedUser(Long reportedUserId);

    int countReportsByReporter(Long reporterId);
}
