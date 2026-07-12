package com.parosurvivors.serviya.reports.application.services;

import com.parosurvivors.serviya.reports.application.dto.command.CreateRequestReportCommand;
import com.parosurvivors.serviya.reports.application.dto.result.ReportDetailResult;
import com.parosurvivors.serviya.reports.application.ports.input.ReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.RequestReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.output.RequestReportPersistencePort;
import com.parosurvivors.serviya.reports.domain.Report;
import com.parosurvivors.serviya.reports.domain.RequestReport;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequestReportService implements RequestReportServicePort {

    private final RequestReportPersistencePort requestReportPersistencePort;
    private final ReportServicePort reportServicePort;

    @Override
    public RequestReport createReport(CreateRequestReportCommand command) {
        Report baseReport = reportServicePort.createBaseReport(
                command.reporterId(),
                command.reportedUserId(),
                "REQUEST",
                command.category(),
                command.reason());

        RequestReport link = RequestReport.builder()
                .reportId(baseReport.getId())
                .requestId(command.requestId())
                .build();

        return requestReportPersistencePort.save(link);
    }

    @Override
    public ReportDetailResult getReportDetail(Long reportId) {
        return reportServicePort.getReportDetail(reportId);
    }
}
