package com.parosurvivors.serviya.reports.application.services;

import com.parosurvivors.serviya.reports.application.dto.command.CreateRequestReportCommand;
import com.parosurvivors.serviya.reports.application.ports.input.ReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.output.RequestReportPersistencePort;
import com.parosurvivors.serviya.reports.domain.Report;
import com.parosurvivors.serviya.reports.domain.ReportPriority;
import com.parosurvivors.serviya.reports.domain.ReportStatus;
import com.parosurvivors.serviya.reports.domain.ReportType;
import com.parosurvivors.serviya.reports.domain.RequestReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestReportServiceTest {

    @Mock
    private RequestReportPersistencePort requestReportPersistencePort;

    @Mock
    private ReportServicePort reportServicePort;

    @InjectMocks
    private RequestReportService service;

    @Test
    void shouldCreateRequestReportAndLinkItToTheBaseReport() {
        CreateRequestReportCommand command = new CreateRequestReportCommand(1L, 2L, "Fraude", "No asistió", 99L);
        Report baseReport = Report.builder()
                .id(10L)
                .reporterId(1L)
                .reportedUserId(2L)
                .reportType(ReportType.REQUEST)
                .category("Fraude")
                .reason("No asistió")
                .status(ReportStatus.PENDING)
                .priority(ReportPriority.MEDIUM)
                .build();
        RequestReport linkedReport = RequestReport.builder()
                .id(55L)
                .reportId(10L)
                .requestId(99L)
                .build();

        when(reportServicePort.createBaseReport(1L, 2L, "REQUEST", "Fraude", "No asistió")).thenReturn(baseReport);
        when(requestReportPersistencePort.save(any(RequestReport.class))).thenReturn(linkedReport);

        RequestReport result = service.createReport(command);

        assertThat(result.getReportId()).isEqualTo(10L);
        assertThat(result.getRequestId()).isEqualTo(99L);
        verify(reportServicePort).createBaseReport(1L, 2L, "REQUEST", "Fraude", "No asistió");
    }
}
