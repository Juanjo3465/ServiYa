package com.parosurvivors.serviya.reports.application.services;

import com.parosurvivors.serviya.reports.application.dto.command.CreateServiceFeedbackReportCommand;
import com.parosurvivors.serviya.reports.application.dto.result.ReportDetailResult;
import com.parosurvivors.serviya.reports.application.ports.input.ReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.ServiceFeedbackReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.output.ServiceFeedbackReportPersistencePort;
import com.parosurvivors.serviya.reports.domain.Report;
import com.parosurvivors.serviya.reports.domain.ServiceFeedbackReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceFeedbackReportService implements ServiceFeedbackReportServicePort {

    private final ServiceFeedbackReportPersistencePort serviceFeedbackReportPersistencePort;
    private final ReportServicePort reportServicePort;

    @Override
    public ServiceFeedbackReport createReport(CreateServiceFeedbackReportCommand command) {
        Report baseReport = reportServicePort.createBaseReport(
                command.reporterId(),
                command.reportedUserId(),
                "SERVICE_FEEDBACK",
                command.category(),
                command.reason());

        ServiceFeedbackReport link = ServiceFeedbackReport.builder()
                .reportId(baseReport.getId())
                .feedbackId(command.serviceFeedbackId())
                .build();

        return serviceFeedbackReportPersistencePort.save(link);
    }

    @Override
    public ReportDetailResult getReportDetail(Long reportId) {
        return reportServicePort.getReportDetail(reportId);
    }
}
