package com.parosurvivors.serviya.reports.application.services;

import com.parosurvivors.serviya.reports.application.dto.command.CreateServiceFeedbackReportCommand;
import com.parosurvivors.serviya.reports.application.dto.result.ReportDetailResult;
import com.parosurvivors.serviya.reports.application.ports.input.ReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.ServiceFeedbackReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.output.ServiceFeedbackReportPersistencePort;
import com.parosurvivors.serviya.reports.domain.ServiceFeedbackReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ServiceFeedbackReportServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ServiceFeedbackReportService implements ServiceFeedbackReportServicePort {

    private final ServiceFeedbackReportPersistencePort serviceFeedbackReportPersistencePort;
    private final ReportServicePort reportServicePort;

    @Override
    public ServiceFeedbackReport createReport(CreateServiceFeedbackReportCommand command) {
        throw new UnsupportedOperationException("TODO: createReport — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public ReportDetailResult getReportDetail(Long reportId) {
        throw new UnsupportedOperationException("TODO: getReportDetail — placeholder, ver estructura-servicios.docx");
    }
}
