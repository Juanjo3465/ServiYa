package com.parosurvivors.serviya.reports.application.ports.input;

import com.parosurvivors.serviya.reports.application.dto.RequestReportDetailResponse;
import com.parosurvivors.serviya.reports.domain.RequestReport;

/**
 * Puerto de entrada de RequestReportService (subtipo REQUEST).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 7).
 */
public interface RequestReportServicePort {

    RequestReport createReport(int reporterId, int reportedUserId, String category, String reason, int requestId);

    RequestReportDetailResponse getReportDetail(int reportId);
}
