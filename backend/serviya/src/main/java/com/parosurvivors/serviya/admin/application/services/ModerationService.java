package com.parosurvivors.serviya.admin.application.services;

import com.parosurvivors.serviya.admin.application.dto.command.RemoveFeedbackCommand;
import com.parosurvivors.serviya.admin.application.ports.input.ModerationServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.ClientFeedbackServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.ServiceFeedbackServicePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ClientFeedback;
import com.parosurvivors.serviya.feedback.domain.ServiceFeedback;
import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.ReportActionServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.ReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.output.ClientFeedbackReportPersistencePort;
import com.parosurvivors.serviya.reports.application.ports.output.RequestReportPersistencePort;
import com.parosurvivors.serviya.reports.application.ports.output.ServiceFeedbackReportPersistencePort;
import com.parosurvivors.serviya.reports.domain.ClientFeedbackReport;
import com.parosurvivors.serviya.reports.domain.RequestReport;
import com.parosurvivors.serviya.reports.domain.ServiceFeedbackReport;
import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestCommandServicePort;
import com.parosurvivors.serviya.shared.exceptions.InvalidStateException;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import com.parosurvivors.serviya.users.application.ports.input.UserServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final ServiceFeedbackReportPersistencePort serviceFeedbackReportPersistencePort;
    private final ClientFeedbackReportPersistencePort clientFeedbackReportPersistencePort;
    private final RequestReportPersistencePort requestReportPersistencePort;
    private final ServiceFeedbackPersistencePort serviceFeedbackPersistencePort;
    private final ClientFeedbackPersistencePort clientFeedbackPersistencePort;

    @Override
    @Transactional
    public void warnUser(Long reportId, Long adminId) {
        var detail = reportServicePort.getReportDetail(reportId);
        if (!"PENDING".equals(detail.status())) {
            throw new InvalidStateException("Solo se puede actuar sobre un reporte pendiente");
        }
        notificationServicePort.notify(
                detail.reportedUserId(),
                "ACCOUNT_WARNING",
                "Advertencia administrativa",
                "Su cuenta ha recibido una advertencia por un reporte.",
                "Report", reportId,
                java.util.List.of(), java.util.Map.of());
        reportServicePort.closeReport(reportId);
        reportActionServicePort.createAction(reportId, adminId, "ADVERTENCIA");
    }

    @Override
    @Transactional
    public void banUserFromReport(Long reportId, Long adminId) {
        var detail = reportServicePort.getReportDetail(reportId);
        if (!"PENDING".equals(detail.status())) {
            throw new InvalidStateException("Solo se puede actuar sobre un reporte pendiente");
        }
        userServicePort.banUser(detail.reportedUserId());
        reportServicePort.closeReport(reportId);
        reportActionServicePort.createAction(reportId, adminId, "BANEO");
    }

    @Override
    @Transactional
    public void revertFeedbackFromReport(Long reportId, Long adminId) {
        var detail = reportServicePort.getReportDetail(reportId);
        if (!"PENDING".equals(detail.status())) {
            throw new InvalidStateException("Solo se puede actuar sobre un reporte pendiente");
        }

        switch (detail.reportType()) {
            case "SERVICE_FEEDBACK" -> {
                ServiceFeedbackReport link = serviceFeedbackReportPersistencePort.findByReportId(reportId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Enlace de feedback de servicio no encontrado para reporte: " + reportId));
                ServiceFeedback feedback = serviceFeedbackPersistencePort.findById(link.getFeedbackId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Feedback de servicio no encontrado: " + link.getFeedbackId()));
                serviceFeedbackServicePort.revertFeedback(feedback.getRequestId());
            }
            case "CLIENT_FEEDBACK" -> {
                ClientFeedbackReport link = clientFeedbackReportPersistencePort.findByReportId(reportId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Enlace de feedback de cliente no encontrado para reporte: " + reportId));
                ClientFeedback feedback = clientFeedbackPersistencePort.findById(link.getFeedbackId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Feedback de cliente no encontrado: " + link.getFeedbackId()));
                clientFeedbackServicePort.revertFeedback(feedback.getRequestId());
            }
            default -> throw new InvalidStateException(
                    "Este reporte no es de tipo feedback: " + detail.reportType());
        }

        reportServicePort.closeReport(reportId);
        reportActionServicePort.createAction(reportId, adminId, "REVERSION_DE_FEEDBACK");
    }

    @Override
    @Transactional
    public void removeFeedbackDirectly(RemoveFeedbackCommand command) {
        Long reportId = createReportAndLink(command.adminId(), command.reportedUserId(),
                command.targetType(), command.targetId(), command.category(), command.reason());

        switch (command.targetType()) {
            case "SERVICE_FEEDBACK" -> {
                ServiceFeedback feedback = serviceFeedbackPersistencePort.findById(command.targetId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Feedback de servicio no encontrado: " + command.targetId()));
                serviceFeedbackServicePort.revertFeedback(feedback.getRequestId());
            }
            case "CLIENT_FEEDBACK" -> {
                ClientFeedback feedback = clientFeedbackPersistencePort.findById(command.targetId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Feedback de cliente no encontrado: " + command.targetId()));
                clientFeedbackServicePort.revertFeedback(feedback.getRequestId());
            }
            default -> throw new InvalidStateException(
                    "Tipo de feedback no soportado: " + command.targetType());
        }

        reportServicePort.closeReport(reportId);
        reportActionServicePort.createAction(reportId, command.adminId(),
                "ELIMINACION_DIRECTA_DE_FEEDBACK");
    }

    @Override
    @Transactional
    public void markRequestAsNotProvided(Long reportId, Long adminId) {
        var detail = reportServicePort.getReportDetail(reportId);
        if (!"PENDING".equals(detail.status())) {
            throw new InvalidStateException("Solo se puede actuar sobre un reporte pendiente");
        }
        if (!"REQUEST".equals(detail.reportType())) {
            throw new InvalidStateException(
                    "Este reporte no es de tipo solicitud: " + detail.reportType());
        }
        RequestReport link = requestReportPersistencePort.findByReportId(reportId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enlace de solicitud no encontrado para reporte: " + reportId));
        serviceRequestCommandServicePort.markAsNotProvided(link.getRequestId(), adminId);
        reportServicePort.closeReport(reportId);
        reportActionServicePort.createAction(reportId, adminId, "SOLICITUD_NO_PRESTADA");
    }

    @Override
    @Transactional
    public void closeReport(Long reportId, Long adminId) {
        var detail = reportServicePort.getReportDetail(reportId);
        if (!"PENDING".equals(detail.status())) {
            throw new InvalidStateException("Solo se puede actuar sobre un reporte pendiente");
        }
        reportServicePort.closeReport(reportId);
        reportActionServicePort.createAction(reportId, adminId, "CIERRE_SIN_PENALIZACION");
    }

    private Long createReportAndLink(Long adminId, Long reportedUserId,
                                     String targetType, Long targetId,
                                     String category, String reason) {
        var baseReport = reportServicePort.createBaseReport(adminId, reportedUserId, targetType, category, reason);

        switch (targetType) {
            case "SERVICE_FEEDBACK" -> serviceFeedbackReportPersistencePort.save(
                    ServiceFeedbackReport.builder()
                            .reportId(baseReport.getId())
                            .feedbackId(targetId)
                            .build());
            case "CLIENT_FEEDBACK" -> clientFeedbackReportPersistencePort.save(
                    ClientFeedbackReport.builder()
                            .reportId(baseReport.getId())
                            .feedbackId(targetId)
                            .build());
            default -> throw new InvalidStateException(
                    "Tipo de feedback no soportado para creacion directa: " + targetType);
        }

        return baseReport.getId();
    }
}
