package com.parosurvivors.serviya.feedback.infrastructure.adapters.output;

import com.parosurvivors.serviya.feedback.application.ports.output.ServiceReviewPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ServiceReview;
import com.parosurvivors.serviya.feedback.infrastructure.entities.ServiceReviewEntity;
import com.parosurvivors.serviya.feedback.infrastructure.mappers.ServiceReviewPersistenceMapper;
import com.parosurvivors.serviya.feedback.infrastructure.repositories.ServiceReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ServiceReviewPersistenceAdapter implements ServiceReviewPersistencePort {

    private final ServiceReviewRepository repository;
    private final ServiceReviewPersistenceMapper mapper;

    @Override
    public ServiceReview save(ServiceReview review) {
        ServiceReviewEntity saved = repository.save(mapper.toEntity(review));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ServiceReview> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ServiceReview> findByRequestId(Long requestId) {
        return repository.findByRequestId(requestId).map(mapper::toDomain);
    }

    @Override
    public List<ServiceReview> findByClientId(Long clientId) {
        return repository.findByClientId(clientId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
