package com.parosurvivors.serviya.reports.infrastructure.adapters.output;

import com.parosurvivors.serviya.reports.application.ports.output.ClientReviewReportPersistencePort;
import com.parosurvivors.serviya.reports.domain.ClientReviewReport;
import com.parosurvivors.serviya.reports.infrastructure.entities.ClientReviewReportEntity;
import com.parosurvivors.serviya.reports.infrastructure.mappers.ClientReviewReportPersistenceMapper;
import com.parosurvivors.serviya.reports.infrastructure.repositories.ClientReviewReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ClientReviewReportPersistenceAdapter implements ClientReviewReportPersistencePort {

    private final ClientReviewReportRepository repository;
    private final ClientReviewReportPersistenceMapper mapper;

    @Override
    public ClientReviewReport save(ClientReviewReport report) {
        ClientReviewReportEntity saved = repository.save(mapper.toEntity(report));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ClientReviewReport> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ClientReviewReport> findByReportId(Long reportId) {
        return repository.findByReportId(reportId).map(mapper::toDomain);
    }
}
