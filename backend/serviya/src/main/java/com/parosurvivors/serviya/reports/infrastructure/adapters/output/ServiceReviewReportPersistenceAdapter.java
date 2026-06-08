package com.parosurvivors.serviya.reports.infrastructure.adapters.output;

import com.parosurvivors.serviya.reports.application.ports.output.ServiceReviewReportPersistencePort;
import com.parosurvivors.serviya.reports.domain.ServiceReviewReport;
import com.parosurvivors.serviya.reports.infrastructure.entities.ServiceReviewReportEntity;
import com.parosurvivors.serviya.reports.infrastructure.mappers.ServiceReviewReportPersistenceMapper;
import com.parosurvivors.serviya.reports.infrastructure.repositories.ServiceReviewReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ServiceReviewReportPersistenceAdapter implements ServiceReviewReportPersistencePort {

    private final ServiceReviewReportRepository repository;
    private final ServiceReviewReportPersistenceMapper mapper;

    @Override
    public ServiceReviewReport save(ServiceReviewReport report) {
        ServiceReviewReportEntity saved = repository.save(mapper.toEntity(report));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ServiceReviewReport> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ServiceReviewReport> findByReportId(Long reportId) {
        return repository.findByReportId(reportId).map(mapper::toDomain);
    }
}
