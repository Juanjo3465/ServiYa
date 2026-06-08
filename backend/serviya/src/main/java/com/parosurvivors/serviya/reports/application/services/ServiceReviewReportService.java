package com.parosurvivors.serviya.reports.application.services;

import com.parosurvivors.serviya.reports.application.dto.ServiceReviewReportDetailResponse;
import com.parosurvivors.serviya.reports.application.ports.input.ReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.input.ServiceReviewReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.output.ServiceReviewReportPersistencePort;
import com.parosurvivors.serviya.reports.domain.ServiceReviewReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ServiceReviewReportServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ServiceReviewReportService implements ServiceReviewReportServicePort {

    private final ServiceReviewReportPersistencePort serviceReviewReportPersistencePort;
    private final ReportServicePort reportServicePort;

    @Override
    public ServiceReviewReport createReport(Long reporterId, Long reportedUserId, String category, String reason, Long serviceReviewId) {
        throw new UnsupportedOperationException("TODO: createReport — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public ServiceReviewReportDetailResponse getReportDetail(Long reportId) {
        throw new UnsupportedOperationException("TODO: getReportDetail — placeholder, ver estructura-servicios.docx");
    }
}
