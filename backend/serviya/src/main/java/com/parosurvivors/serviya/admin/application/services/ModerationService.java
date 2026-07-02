package com.parosurvivors.serviya.admin.application.services;

import com.parosurvivors.serviya.admin.application.dto.command.RemoveFeedbackCommand;
import com.parosurvivors.serviya.admin.application.ports.input.ModerationServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.ClientFeedbackServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.ServiceFeedbackServicePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackPersistencePort;
import com.parosurvivors.serviya.reports.application.dto.result.ReportDetailResult;
import com.parosurvivors.serviya.reports.application.ports.input.ReportActionServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.ReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.output.ClientFeedbackReportPersistencePort;
import com.parosurvivors.serviya.reports.application.ports.output.ReportPersistencePort;
import com.parosurvivors.serviya.reports.application.ports.output.ServiceFeedbackReportPersistencePort;
import com.parosurvivors.serviya.reports.domain.ClientFeedbackReport;
import com.parosurvivors.serviya.reports.domain.Report;
import com.parosurvivors.serviya.reports.domain.ReportType;
import com.parosurvivors.serviya.reports.domain.ServiceFeedbackReport;
import com.parosurvivors.serviya.shared.exceptions.InvalidStateException;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ModerationService implements ModerationServicePort {

    private final ReportServicePort reportServicePort;
    private final ReportActionServicePort reportActionServicePort;
    private final ReportPersistencePort reportPersistencePort;
    private final ServiceFeedbackServicePort serviceFeedbackServicePort;
    private final ClientFeedbackServicePort clientFeedbackServicePort;
    private final ServiceFeedbackPersistencePort serviceFeedbackPersistencePort;
    private final ClientFeedbackPersistencePort clientFeedbackPersistencePort;
    private final ServiceFeedbackReportPersistencePort serviceFeedbackReportPersistencePort;
    private final ClientFeedbackReportPersistencePort clientFeedbackReportPersistencePort;

    @Override
    public void warnUser(Long reportId, Long adminId) {
        throw new UnsupportedOperationException("TODO: warnUser — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void banUserFromReport(Long reportId, Long adminId) {
        throw new UnsupportedOperationException("TODO: banUserFromReport — placeholder, ver estructura-servicios.docx");
    }

    @Override
    @Transactional
    public void revertFeedbackFromReport(Long reportId, Long adminId) {
        ReportDetailResult detail = reportServicePort.getReportDetail(reportId);
        revertFeedbackByDetail(detail);
        reportActionServicePort.createAction(reportId, adminId, "REVERT_FEEDBACK");
        finalizeReport(reportId);
    }

    @Override
    @Transactional
    public void removeFeedbackDirectly(RemoveFeedbackCommand command) {
        ReportType reportType = ReportType.valueOf(command.targetType());
        Report report = reportServicePort.createBaseReport(
                command.adminId(),
                command.reportedUserId(),
                command.targetType(),
                command.category(),
                command.reason() != null ? command.reason() : "Eliminación directa por admin");

        if (reportType == ReportType.SERVICE_FEEDBACK) {
            Long feedbackId = serviceFeedbackPersistencePort.findByRequestId(command.targetId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Service feedback not found for requestId: " + command.targetId()))
                    .getId();
            serviceFeedbackReportPersistencePort.save(ServiceFeedbackReport.builder()
                    .reportId(report.getId())
                    .feedbackId(feedbackId)
                    .build());
        } else if (reportType == ReportType.CLIENT_FEEDBACK) {
            Long feedbackId = clientFeedbackPersistencePort.findByRequestId(command.targetId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Client feedback not found for requestId: " + command.targetId()))
                    .getId();
            clientFeedbackReportPersistencePort.save(ClientFeedbackReport.builder()
                    .reportId(report.getId())
                    .feedbackId(feedbackId)
                    .build());
        } else {
            throw new InvalidStateException("Tipo de reporte no soportado para eliminar feedback: " + reportType);
        }

        revertFeedbackFromReport(report.getId(), command.adminId());
    }

    @Override
    public void markRequestAsNotProvided(Long reportId, Long adminId) {
        throw new UnsupportedOperationException("TODO: markRequestAsNotProvided — placeholder, ver estructura-servicios.docx");
    }

    @Override
    @Transactional
    public void closeReport(Long reportId, Long adminId) {
        finalizeReport(reportId);
        reportActionServicePort.createAction(reportId, adminId, "CLOSE");
    }

    private void revertFeedbackByDetail(ReportDetailResult detail) {
        if (ReportType.SERVICE_FEEDBACK.name().equals(detail.reportType())) {
            if (detail.serviceFeedbackId() == null) {
                throw new ResourceNotFoundException("Service feedback extension missing for report");
            }
            Long requestId = serviceFeedbackPersistencePort.findById(detail.serviceFeedbackId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service feedback not found"))
                    .getRequestId();
            serviceFeedbackServicePort.revertFeedback(requestId);
            return;
        }
        if (ReportType.CLIENT_FEEDBACK.name().equals(detail.reportType())) {
            if (detail.clientFeedbackId() == null) {
                throw new ResourceNotFoundException("Client feedback extension missing for report");
            }
            Long requestId = clientFeedbackPersistencePort.findById(detail.clientFeedbackId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client feedback not found"))
                    .getRequestId();
            clientFeedbackServicePort.revertFeedback(requestId);
            return;
        }
        throw new InvalidStateException("El reporte no es de tipo feedback: " + detail.reportType());
    }

    private void finalizeReport(Long reportId) {
        Report report = reportPersistencePort.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));
        report.resolve();
        reportPersistencePort.update(report);
    }
}
