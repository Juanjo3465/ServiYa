package com.parosurvivors.serviya.reports.application.ports.input;

import com.parosurvivors.serviya.reports.application.dto.ClientReviewReportDetailResponse;
import com.parosurvivors.serviya.reports.domain.ClientReviewReport;

/**
 * Puerto de entrada de ClientReviewReportService (subtipo CLIENT_REVIEW).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 7).
 */
public interface ClientReviewReportServicePort {

    ClientReviewReport createReport(Long reporterId, Long reportedUserId, String category, String reason, Long clientReviewId);

    ClientReviewReportDetailResponse getReportDetail(Long reportId);
}
