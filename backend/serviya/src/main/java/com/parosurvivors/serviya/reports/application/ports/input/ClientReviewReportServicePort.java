package com.parosurvivors.serviya.reports.application.ports.input;

import com.parosurvivors.serviya.reports.application.dto.command.CreateClientReviewReportCommand;
import com.parosurvivors.serviya.reports.application.dto.result.ReportDetailResult;
import com.parosurvivors.serviya.reports.domain.ClientReviewReport;

/**
 * Puerto de entrada de ClientReviewReportService (subtipo CLIENT_REVIEW). createReport recibe Command y
 * devuelve el dominio del enlace; el detalle devuelve el Result paraguas. Nunca tipos web.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 7).
 */
public interface ClientReviewReportServicePort {

    ClientReviewReport createReport(CreateClientReviewReportCommand command);

    ReportDetailResult getReportDetail(Long reportId);
}
