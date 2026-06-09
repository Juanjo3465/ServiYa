package com.parosurvivors.serviya.feedback.infrastructure.adapters.output;

import com.parosurvivors.serviya.feedback.application.ports.output.ClientReviewTagCatalogPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ClientReviewTagCatalog;
import com.parosurvivors.serviya.feedback.infrastructure.mappers.ClientReviewTagCatalogPersistenceMapper;
import com.parosurvivors.serviya.feedback.infrastructure.repositories.ClientReviewTagCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClientReviewTagCatalogPersistenceAdapter implements ClientReviewTagCatalogPersistencePort {

    private final ClientReviewTagCatalogRepository repository;
    private final ClientReviewTagCatalogPersistenceMapper mapper;

    @Override
    public List<ClientReviewTagCatalog> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ClientReviewTagCatalog> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
