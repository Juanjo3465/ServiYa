package com.parosurvivors.serviya.reports.application.services;

import com.parosurvivors.serviya.reports.application.dto.ReportDetailResponse;
import com.parosurvivors.serviya.reports.application.dto.ReportResponse;
import com.parosurvivors.serviya.reports.application.ports.input.ReportServicePort;
import com.parosurvivors.serviya.reports.application.ports.output.ReportPersistencePort;
import com.parosurvivors.serviya.reports.domain.Report;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ReportServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ReportService implements ReportServicePort {

    private final ReportPersistencePort reportPersistencePort;

    @Override
    public Report createBaseReport(Long reporterId, Long reportedUserId, String type, String category, String reason) {
        throw new UnsupportedOperationException("TODO: createBaseReport — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public ReportDetailResponse getReportDetail(Long reportId) {
        throw new UnsupportedOperationException("TODO: getReportDetail — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Page<ReportResponse> getReports(String type, String category, String status, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: getReports — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public List<ReportResponse> getReportsByReporter(Long reporterId) {
        throw new UnsupportedOperationException("TODO: getReportsByReporter — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public List<ReportResponse> getReportsByReportedUser(Long reportedUserId) {
        throw new UnsupportedOperationException("TODO: getReportsByReportedUser — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public int countReportsByReportedUser(Long reportedUserId) {
        throw new UnsupportedOperationException("TODO: countReportsByReportedUser — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public int countReportsByReporter(Long reporterId) {
        throw new UnsupportedOperationException("TODO: countReportsByReporter — placeholder, ver estructura-servicios.docx");
    }
}
