package com.parosurvivors.serviya.reports.application.services;

import com.parosurvivors.serviya.reports.application.dto.command.CreateClientFeedbackReportCommand;
import com.parosurvivors.serviya.reports.application.dto.result.ReportDetailResult;
import com.parosurvivors.serviya.reports.application.ports.input.ClientFeedbackReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.ReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.output.ClientFeedbackReportPersistencePort;
import com.parosurvivors.serviya.reports.domain.ClientFeedbackReport;
import com.parosurvivors.serviya.reports.domain.Report;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientFeedbackReportService implements ClientFeedbackReportServicePort {

    private final ClientFeedbackReportPersistencePort clientFeedbackReportPersistencePort;
    private final ReportServicePort reportServicePort;

    @Override
    public ClientFeedbackReport createReport(CreateClientFeedbackReportCommand command) {
        Report baseReport = reportServicePort.createBaseReport(
                command.reporterId(),
                command.reportedUserId(),
                "CLIENT_FEEDBACK",
                command.category(),
                command.reason());

        ClientFeedbackReport link = ClientFeedbackReport.builder()
                .reportId(baseReport.getId())
                .feedbackId(command.clientFeedbackId())
                .build();

        return clientFeedbackReportPersistencePort.save(link);
    }

    @Override
    public ReportDetailResult getReportDetail(Long reportId) {
        return reportServicePort.getReportDetail(reportId);
    }
}
