package com.parosurvivors.serviya.reports.application.services;

import com.parosurvivors.serviya.feedback.application.dto.result.ClientFeedbackResult;
import com.parosurvivors.serviya.feedback.application.dto.result.ServiceFeedbackResult;
import com.parosurvivors.serviya.feedback.application.ports.input.ClientFeedbackServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.ServiceFeedbackServicePort;
import com.parosurvivors.serviya.profiles.application.ports.input.UserProfileServicePort;
import com.parosurvivors.serviya.profiles.domain.UserProfile;
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
import com.parosurvivors.serviya.requests.application.dto.result.AdminRequestDetailResult;
import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestQueryServicePort;
import com.parosurvivors.serviya.requests.domain.RequestStatus;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ReportPersistencePort reportPersistencePort;
    @Mock private RequestReportPersistencePort requestReportPersistencePort;
    @Mock private ServiceFeedbackReportPersistencePort serviceFeedbackReportPersistencePort;
    @Mock private ClientFeedbackReportPersistencePort clientFeedbackReportPersistencePort;
    @Mock private UserProfileServicePort userProfileServicePort;
    @Mock private ServiceRequestQueryServicePort serviceRequestQueryServicePort;
    @Mock private ServiceFeedbackServicePort serviceFeedbackServicePort;
    @Mock private ClientFeedbackServicePort clientFeedbackServicePort;

    @InjectMocks
    private ReportService service;

    private Report report(Long id, ReportType type) {
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

    private void stubParties() {
        when(userProfileServicePort.getProfileInfo(8L))
                .thenReturn(UserProfile.builder().userId(8L).fullName("Rep Orter").profilePhotoUrl("r.png").build());
        when(userProfileServicePort.getProfileInfo(45L))
                .thenReturn(UserProfile.builder().userId(45L).fullName("Report Ed").profilePhotoUrl("e.png").build());
    }

    private AdminRequestDetailResult adminRequest(Long requestId) {
        return new AdminRequestDetailResult(
                requestId, RequestStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0), new BigDecimal("50000"),
                null, null, null, null, null,
                7L, "Plomería a domicilio", "Hogar", new BigDecimal("30000"), 60,
                null, null, null, null, null, null,
                null, null, "Bogotá", null, null);
    }

    @Test
    void getReportDetailEnrichesPartiesAndRequestPayload() {
        when(reportPersistencePort.findById(1L)).thenReturn(Optional.of(report(1L, ReportType.REQUEST)));
        stubParties();
        when(requestReportPersistencePort.findByReportId(1L))
                .thenReturn(Optional.of(RequestReport.builder().reportId(1L).requestId(99L).build()));
        when(serviceRequestQueryServicePort.getRequestDetailForAdmin(99L)).thenReturn(adminRequest(99L));

        ReportDetailResult result = service.getReportDetail(1L);

        assertThat(result.reporter().fullName()).isEqualTo("Rep Orter");
        assertThat(result.reporter().photoUrl()).isEqualTo("r.png");
        assertThat(result.reported().fullName()).isEqualTo("Report Ed");
        assertThat(result.request().requestId()).isEqualTo(99L);
        assertThat(result.request().serviceTitle()).isEqualTo("Plomería a domicilio");
        assertThat(result.request().status()).isEqualTo("PENDING");
        assertThat(result.request().city()).isEqualTo("Bogotá");
        assertThat(result.feedback()).isNull();
    }

    @Test
    void getReportDetailEnrichesServiceFeedbackContent() {
        when(reportPersistencePort.findById(2L)).thenReturn(Optional.of(report(2L, ReportType.SERVICE_FEEDBACK)));
        stubParties();
        when(serviceFeedbackReportPersistencePort.findByReportId(2L))
                .thenReturn(Optional.of(ServiceFeedbackReport.builder().reportId(2L).feedbackId(77L).build()));
        when(serviceFeedbackServicePort.getServiceFeedbackById(77L)).thenReturn(Optional.of(
                new ServiceFeedbackResult(99L, 7L, 8L, 1, "pésimo", List.of("impuntual"), LocalDateTime.now())));

        ReportDetailResult result = service.getReportDetail(2L);

        assertThat(result.request()).isNull();
        assertThat(result.feedback().kind()).isEqualTo("SERVICE");
        assertThat(result.feedback().feedbackId()).isEqualTo(77L);
        assertThat(result.feedback().rating()).isEqualTo(1);
        assertThat(result.feedback().comment()).isEqualTo("pésimo");
        assertThat(result.feedback().tags()).containsExactly("impuntual");
    }

    @Test
    void getReportDetailForRevertedFeedbackReturnsShellWithIdAndKindOnly() {
        when(reportPersistencePort.findById(3L)).thenReturn(Optional.of(report(3L, ReportType.CLIENT_FEEDBACK)));
        stubParties();
        when(clientFeedbackReportPersistencePort.findByReportId(3L))
                .thenReturn(Optional.of(ClientFeedbackReport.builder().reportId(3L).feedbackId(55L).build()));
        when(clientFeedbackServicePort.getClientFeedbackById(55L)).thenReturn(Optional.empty());

        ReportDetailResult result = service.getReportDetail(3L);

        assertThat(result.feedback().kind()).isEqualTo("CLIENT");
        assertThat(result.feedback().feedbackId()).isEqualTo(55L);
        assertThat(result.feedback().rating()).isNull();
        assertThat(result.feedback().comment()).isNull();
        assertThat(result.feedback().tags()).isEmpty();
    }

    @Test
    void getReportDetailThrowsWhenReportMissing() {
        when(reportPersistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReportDetail(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
