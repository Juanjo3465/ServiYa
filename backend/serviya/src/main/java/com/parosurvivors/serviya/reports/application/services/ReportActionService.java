package com.parosurvivors.serviya.reports.application.services;

import com.parosurvivors.serviya.reports.application.ports.input.ReportActionServicePort;
import com.parosurvivors.serviya.reports.application.ports.output.ReportActionPersistencePort;
import com.parosurvivors.serviya.reports.domain.ReportAction;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportActionService implements ReportActionServicePort {

    private final ReportActionPersistencePort reportActionPersistencePort;

    @Override
    public ReportAction createAction(Long reportId, Long adminId, String actionTaken) {
        ReportAction action = ReportAction.builder()
                .reportId(reportId)
                .adminId(adminId)
                .actionDescription(actionTaken)
                .createdAt(LocalDateTime.now())
                .build();
        return reportActionPersistencePort.save(action);
    }

    @Override
    public List<ReportAction> getActionsByReport(Long reportId) {
        return reportActionPersistencePort.findByReportId(reportId);
    }
}
