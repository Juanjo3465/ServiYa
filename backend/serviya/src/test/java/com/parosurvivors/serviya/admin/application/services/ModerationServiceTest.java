package com.parosurvivors.serviya.admin.application.services;

import com.parosurvivors.serviya.admin.application.dto.command.RemoveFeedbackCommand;
import com.parosurvivors.serviya.feedback.application.dto.result.ClientFeedbackResult;
import com.parosurvivors.serviya.feedback.application.dto.result.ServiceFeedbackResult;
import com.parosurvivors.serviya.feedback.application.ports.input.ClientFeedbackServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.ServiceFeedbackServicePort;
import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.ClientFeedbackReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.ReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.ServiceFeedbackReportServicePort;
import com.parosurvivors.serviya.reports.domain.ReportActionType;
import com.parosurvivors.serviya.reports.domain.ReportStatus;
import com.parosurvivors.serviya.reports.domain.ReportSummary;
import com.parosurvivors.serviya.reports.domain.ReportType;
import com.parosurvivors.serviya.reports.domain.ServiceFeedbackReport;
import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestCommandServicePort;
import com.parosurvivors.serviya.shared.exceptions.InvalidStateException;
import com.parosurvivors.serviya.users.application.ports.input.UserServicePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock private ReportServicePort reportServicePort;
    @Mock private ServiceFeedbackServicePort serviceFeedbackServicePort;
    @Mock private ClientFeedbackServicePort clientFeedbackServicePort;
    @Mock private ServiceFeedbackReportServicePort serviceFeedbackReportServicePort;
    @Mock private ClientFeedbackReportServicePort clientFeedbackReportServicePort;
    @Mock private ServiceRequestCommandServicePort serviceRequestCommandServicePort;
    @Mock private UserServicePort userServicePort;
    @Mock private NotificationServicePort notificationServicePort;

    @InjectMocks
    private ModerationService service;

    private static final Long ADMIN = 100L;
    private static final Long REPORTED = 45L;

    private ReportSummary serviceFeedbackReport(Long reportId, Long feedbackId) {
        return new ReportSummary(reportId, 8L, REPORTED, ReportType.SERVICE_FEEDBACK, "SPAM",
                ReportStatus.PENDING, null, feedbackId, null);
    }

    private ReportSummary clientFeedbackReport(Long reportId, Long feedbackId) {
        return new ReportSummary(reportId, 8L, REPORTED, ReportType.CLIENT_FEEDBACK, "SPAM",
                ReportStatus.PENDING, null, null, feedbackId);
    }

    private ReportSummary requestReport(Long reportId, Long requestId) {
        return new ReportSummary(reportId, 8L, REPORTED, ReportType.REQUEST, "SPAM",
                ReportStatus.PENDING, requestId, null, null);
    }

    @Test
    void warnUserNotifiesReportedAndResolvesWithWarn() {
        when(reportServicePort.getReportSummary(1L)).thenReturn(requestReport(1L, 99L));

        service.warnUser(1L, ADMIN);

        verify(notificationServicePort).notify(eq(REPORTED), eq("USER_WARNED"), anyString(), anyString(),
                eq("REPORT"), eq(1L), isNull(), isNull());
        verify(reportServicePort).resolveReport(1L, ADMIN, ReportActionType.WARN);
        verify(userServicePort, never()).banUser(any());
    }

    @Test
    void banUserBansAndResolvesWithoutNotifyingItself() {
        when(reportServicePort.getReportSummary(2L)).thenReturn(requestReport(2L, 99L));

        service.banUserFromReport(2L, ADMIN);

        verify(userServicePort).banUser(REPORTED);
        verify(reportServicePort).resolveReport(2L, ADMIN, ReportActionType.BAN);
        // La notificación al baneado la hace UserService.banUser, no ModerationService.
        verify(notificationServicePort, never()).notify(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void markRequestAsNotProvidedTransitionsRequestAndResolves() {
        when(reportServicePort.getReportSummary(3L)).thenReturn(requestReport(3L, 99L));

        service.markRequestAsNotProvided(3L, ADMIN);

        verify(serviceRequestCommandServicePort).markAsNotProvided(99L, ADMIN);
        verify(reportServicePort).resolveReport(3L, ADMIN, ReportActionType.MARK_REQUEST_NOT_PROVIDED);
    }

    @Test
    void markRequestAsNotProvidedFailsWhenNotARequestReport() {
        when(reportServicePort.getReportSummary(4L)).thenReturn(serviceFeedbackReport(4L, 77L));

        assertThatThrownBy(() -> service.markRequestAsNotProvided(4L, ADMIN))
                .isInstanceOf(InvalidStateException.class);
        verify(serviceRequestCommandServicePort, never()).markAsNotProvided(any(), any());
        verify(reportServicePort, never()).resolveReport(any(), any(), any());
    }

    @Test
    void closeReportDelegatesToFinalize() {
        service.closeReport(5L, ADMIN);
        verify(reportServicePort).closeReport(5L, ADMIN);
    }

    @Test
    void revertServiceFeedbackResolvesWithRevert() {
        when(reportServicePort.getReportSummary(6L)).thenReturn(serviceFeedbackReport(6L, 77L));
        when(serviceFeedbackServicePort.getServiceFeedbackById(77L)).thenReturn(Optional.of(
                new ServiceFeedbackResult(99L, 7L, 8L, 1, "malo", List.of(), null)));

        service.revertFeedbackFromReport(6L, ADMIN);

        verify(serviceFeedbackServicePort).revertFeedback(99L);
        verify(reportServicePort).resolveReport(6L, ADMIN, ReportActionType.REVERT_FEEDBACK);
    }

    @Test
    void revertClientFeedbackResolvesWithRevert() {
        when(reportServicePort.getReportSummary(7L)).thenReturn(clientFeedbackReport(7L, 88L));
        when(clientFeedbackServicePort.getClientFeedbackById(88L)).thenReturn(Optional.of(
                new ClientFeedbackResult(99L, 8L, 45L, 1, "malo", List.of(), null)));

        service.revertFeedbackFromReport(7L, ADMIN);

        verify(clientFeedbackServicePort).revertFeedback(99L);
        verify(reportServicePort).resolveReport(7L, ADMIN, ReportActionType.REVERT_FEEDBACK);
    }

    @Test
    void revertFailsWhenFeedbackAlreadyReverted() {
        when(reportServicePort.getReportSummary(8L)).thenReturn(serviceFeedbackReport(8L, 77L));
        when(serviceFeedbackServicePort.getServiceFeedbackById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revertFeedbackFromReport(8L, ADMIN))
                .isInstanceOf(InvalidStateException.class);
        verify(reportServicePort, never()).resolveReport(any(), any(), any());
    }

    @Test
    void removeFeedbackDirectlyCreatesReportThenReverts() {
        RemoveFeedbackCommand command = new RemoveFeedbackCommand(ADMIN, "SERVICE", 77L, REPORTED, "SPAM", "ofensivo");
        when(serviceFeedbackReportServicePort.createReport(any())).thenReturn(
                ServiceFeedbackReport.builder().reportId(9L).feedbackId(77L).build());
        // revertFeedbackFromReport(9L, ...) internamente vuelve a leer el detalle.
        when(reportServicePort.getReportSummary(9L)).thenReturn(serviceFeedbackReport(9L, 77L));
        when(serviceFeedbackServicePort.getServiceFeedbackById(77L)).thenReturn(Optional.of(
                new ServiceFeedbackResult(99L, 7L, 8L, 1, "malo", List.of(), null)));

        service.removeFeedbackDirectly(command);

        verify(serviceFeedbackServicePort).revertFeedback(99L);
        verify(reportServicePort).resolveReport(9L, ADMIN, ReportActionType.REVERT_FEEDBACK);
    }

    @Test
    void removeFeedbackDirectlyFailsOnInvalidTargetType() {
        RemoveFeedbackCommand command = new RemoveFeedbackCommand(ADMIN, "FOO", 77L, REPORTED, "SPAM", "x");

        assertThatThrownBy(() -> service.removeFeedbackDirectly(command))
                .isInstanceOf(InvalidStateException.class);
    }
}
