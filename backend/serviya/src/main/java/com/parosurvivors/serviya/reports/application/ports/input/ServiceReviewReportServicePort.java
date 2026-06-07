package com.parosurvivors.serviya.reports.application.ports.input;

import com.parosurvivors.serviya.reports.application.dto.ServiceReviewReportDetailResponse;
import com.parosurvivors.serviya.reports.domain.ServiceReviewReport;

/**
 * Puerto de entrada de ServiceReviewReportService (subtipo SERVICE_REVIEW).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 7).
 */
public interface ServiceReviewReportServicePort {

    ServiceReviewReport createReport(Long reporterId, Long reportedUserId, String category, String reason, Long serviceReviewId);

    ServiceReviewReportDetailResponse getReportDetail(Long reportId);
}
