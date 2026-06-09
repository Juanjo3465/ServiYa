package com.parosurvivors.serviya.admin.application.services;

import com.parosurvivors.serviya.admin.application.dto.command.RemoveFeedbackCommand;
import com.parosurvivors.serviya.admin.application.ports.input.ModerationServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.ClientFeedbackServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.ServiceFeedbackServicePort;
import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.ReportActionServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.ReportServicePort;
import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestCommandServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ModerationServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ModerationService implements ModerationServicePort {

    private final ReportServicePort reportServicePort;
    private final ReportActionServicePort reportActionServicePort;
    private final ServiceFeedbackServicePort serviceFeedbackServicePort;
    private final ClientFeedbackServicePort clientFeedbackServicePort;
    private final ServiceRequestCommandServicePort serviceRequestCommandServicePort;
    private final UserServicePort userServicePort;
    private final NotificationServicePort notificationServicePort;

    @Override
    public void warnUser(Long reportId, Long adminId) {
        throw new UnsupportedOperationException("TODO: warnUser — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void banUserFromReport(Long reportId, Long adminId) {
        throw new UnsupportedOperationException("TODO: banUserFromReport — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void revertFeedbackFromReport(Long reportId, Long adminId) {
        throw new UnsupportedOperationException("TODO: revertFeedbackFromReport — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void removeFeedbackDirectly(RemoveFeedbackCommand command) {
        throw new UnsupportedOperationException("TODO: removeFeedbackDirectly — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void markRequestAsNotProvided(Long reportId, Long adminId) {
        throw new UnsupportedOperationException("TODO: markRequestAsNotProvided — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void closeReport(Long reportId, Long adminId) {
        throw new UnsupportedOperationException("TODO: closeReport — placeholder, ver estructura-servicios.docx");
    }
}
