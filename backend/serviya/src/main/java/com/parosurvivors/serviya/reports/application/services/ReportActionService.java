package com.parosurvivors.serviya.reports.application.services;

import com.parosurvivors.serviya.reports.application.ports.input.ReportActionServicePort;
import com.parosurvivors.serviya.reports.application.ports.output.ReportActionPersistencePort;
import com.parosurvivors.serviya.reports.domain.ReportAction;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ReportActionServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ReportActionService implements ReportActionServicePort {

    private final ReportActionPersistencePort reportActionPersistencePort;

    @Override
    public ReportAction createAction(Long reportId, Long adminId, String actionTaken) {
        throw new UnsupportedOperationException("TODO: createAction — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public List<ReportAction> getActionsByReport(Long reportId) {
        throw new UnsupportedOperationException("TODO: getActionsByReport — placeholder, ver estructura-servicios.docx");
    }
}
