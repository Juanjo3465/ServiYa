package com.parosurvivors.serviya.feedback.infrastructure.adapters.output;

import com.parosurvivors.serviya.feedback.application.ports.output.ServiceReviewTagCatalogPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ServiceReviewTagCatalog;
import com.parosurvivors.serviya.feedback.infrastructure.mappers.ServiceReviewTagCatalogPersistenceMapper;
import com.parosurvivors.serviya.feedback.infrastructure.repositories.ServiceReviewTagCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ServiceReviewTagCatalogPersistenceAdapter implements ServiceReviewTagCatalogPersistencePort {

    private final ServiceReviewTagCatalogRepository repository;
    private final ServiceReviewTagCatalogPersistenceMapper mapper;

    @Override
    public List<ServiceReviewTagCatalog> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ServiceReviewTagCatalog> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
