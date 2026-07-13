package com.parosurvivors.serviya.reports.application.services;

import com.parosurvivors.serviya.reports.application.dto.result.ReportDetailResult;
import com.parosurvivors.serviya.reports.application.ports.output.ClientFeedbackReportPersistencePort;
import com.parosurvivors.serviya.reports.application.ports.output.ReportPersistencePort;
import com.parosurvivors.serviya.reports.application.ports.output.RequestReportPersistencePort;
import com.parosurvivors.serviya.reports.application.ports.output.ServiceFeedbackReportPersistencePort;
import com.parosurvivors.serviya.reports.domain.ClientFeedbackReport;
import com.parosurvivors.serviya.reports.domain.Report;
import com.parosurvivors.serviya.reports.domain.ReportStatus;
import com.parosurvivors.serviya.reports.domain.ReportType;
import com.parosurvivors.serviya.reports.domain.RequestReport;
import com.parosurvivors.serviya.reports.domain.ServiceFeedbackReport;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportPersistencePort reportPersistencePort;
    @Mock
    private RequestReportPersistencePort requestReportPersistencePort;
    @Mock
    private ServiceFeedbackReportPersistencePort serviceFeedbackReportPersistencePort;
    @Mock
    private ClientFeedbackReportPersistencePort clientFeedbackReportPersistencePort;

    @InjectMocks
    private ReportService service;

    private Report baseReport(Long id, ReportType type) {
        return Report.builder()
                .id(id)
                .reporterId(8L)
                .reportedUserId(45L)
                .reportType(type)
                .category("HARASSMENT")
                .reason("razón")
                .status(ReportStatus.PENDING)
                .build();
    }

    @Test
    void getReportDetailFillsRequestIdForRequestReport() {
        when(reportPersistencePort.findById(1L)).thenReturn(Optional.of(baseReport(1L, ReportType.REQUEST)));
        when(requestReportPersistencePort.findByReportId(1L))
                .thenReturn(Optional.of(RequestReport.builder().reportId(1L).requestId(99L).build()));

        ReportDetailResult result = service.getReportDetail(1L);

        assertThat(result.reportType()).isEqualTo("REQUEST");
        assertThat(result.requestId()).isEqualTo(99L);
        assertThat(result.serviceFeedbackId()).isNull();
        assertThat(result.clientFeedbackId()).isNull();
    }

    @Test
    void getReportDetailFillsServiceFeedbackId() {
        when(reportPersistencePort.findById(2L)).thenReturn(Optional.of(baseReport(2L, ReportType.SERVICE_FEEDBACK)));
        when(serviceFeedbackReportPersistencePort.findByReportId(2L))
                .thenReturn(Optional.of(ServiceFeedbackReport.builder().reportId(2L).feedbackId(77L).build()));

        ReportDetailResult result = service.getReportDetail(2L);

        assertThat(result.serviceFeedbackId()).isEqualTo(77L);
        assertThat(result.requestId()).isNull();
        assertThat(result.clientFeedbackId()).isNull();
    }

    @Test
    void getReportDetailFillsClientFeedbackId() {
        when(reportPersistencePort.findById(3L)).thenReturn(Optional.of(baseReport(3L, ReportType.CLIENT_FEEDBACK)));
        when(clientFeedbackReportPersistencePort.findByReportId(3L))
                .thenReturn(Optional.of(ClientFeedbackReport.builder().reportId(3L).feedbackId(55L).build()));

        ReportDetailResult result = service.getReportDetail(3L);

        assertThat(result.clientFeedbackId()).isEqualTo(55L);
        assertThat(result.requestId()).isNull();
        assertThat(result.serviceFeedbackId()).isNull();
    }

    @Test
    void getReportDetailThrowsWhenReportMissing() {
        when(reportPersistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReportDetail(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
