package com.parosurvivors.serviya.reports.application.services;

import com.parosurvivors.serviya.reports.application.dto.command.CreateRequestReportCommand;
import com.parosurvivors.serviya.reports.application.dto.result.ReportDetailResult;
import com.parosurvivors.serviya.reports.application.ports.input.ReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.RequestReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.output.RequestReportPersistencePort;
import com.parosurvivors.serviya.reports.domain.RequestReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de RequestReportServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class RequestReportService implements RequestReportServicePort {

    private final RequestReportPersistencePort requestReportPersistencePort;
    private final ReportServicePort reportServicePort;

    @Override
    public RequestReport createReport(CreateRequestReportCommand command) {
        throw new UnsupportedOperationException("TODO: createReport — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public ReportDetailResult getReportDetail(Long reportId) {
        throw new UnsupportedOperationException("TODO: getReportDetail — placeholder, ver estructura-servicios.docx");
    }
}
