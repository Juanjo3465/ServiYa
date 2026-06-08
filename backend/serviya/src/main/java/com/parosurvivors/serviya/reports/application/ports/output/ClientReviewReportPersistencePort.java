package com.parosurvivors.serviya.reports.application.ports.output;

import com.parosurvivors.serviya.reports.domain.ClientReviewReport;

import java.util.Optional;

public interface ClientReviewReportPersistencePort {
    ClientReviewReport save(ClientReviewReport report);
    Optional<ClientReviewReport> findById(Long id);
    Optional<ClientReviewReport> findByReportId(Long reportId);
}
