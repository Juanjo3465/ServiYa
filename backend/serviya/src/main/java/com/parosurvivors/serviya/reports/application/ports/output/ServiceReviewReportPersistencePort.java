package com.parosurvivors.serviya.reports.application.ports.output;

import com.parosurvivors.serviya.reports.domain.ServiceReviewReport;

import java.util.Optional;

public interface ServiceReviewReportPersistencePort {
    ServiceReviewReport save(ServiceReviewReport report);
    Optional<ServiceReviewReport> findById(Long id);
    Optional<ServiceReviewReport> findByReportId(Long reportId);
}
