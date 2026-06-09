package com.parosurvivors.serviya.reports.application.ports.input;

import com.parosurvivors.serviya.reports.application.dto.command.CreateServiceReviewReportCommand;
import com.parosurvivors.serviya.reports.application.dto.result.ReportDetailResult;
import com.parosurvivors.serviya.reports.domain.ServiceReviewReport;

/**
 * Puerto de entrada de ServiceReviewReportService (subtipo SERVICE_REVIEW). createReport recibe Command y
 * devuelve el dominio del enlace; el detalle devuelve el Result paraguas. Nunca tipos web.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 7).
 */
public interface ServiceReviewReportServicePort {

    ServiceReviewReport createReport(CreateServiceReviewReportCommand command);

    ReportDetailResult getReportDetail(Long reportId);
}
