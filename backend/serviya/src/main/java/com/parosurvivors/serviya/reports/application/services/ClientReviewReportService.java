package com.parosurvivors.serviya.reports.application.services;

import com.parosurvivors.serviya.reports.application.dto.ClientReviewReportDetailResponse;
import com.parosurvivors.serviya.reports.application.ports.input.ClientReviewReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.ReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.output.ClientReviewReportPersistencePort;
import com.parosurvivors.serviya.reports.domain.ClientReviewReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ClientReviewReportServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ClientReviewReportService implements ClientReviewReportServicePort {

    private final ClientReviewReportPersistencePort clientReviewReportPersistencePort;
    private final ReportServicePort reportServicePort;

    @Override
    public ClientReviewReport createReport(Long reporterId, Long reportedUserId, String category, String reason, Long clientReviewId) {
        throw new UnsupportedOperationException("TODO: createReport — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public ClientReviewReportDetailResponse getReportDetail(Long reportId) {
        throw new UnsupportedOperationException("TODO: getReportDetail — placeholder, ver estructura-servicios.docx");
    }
}
