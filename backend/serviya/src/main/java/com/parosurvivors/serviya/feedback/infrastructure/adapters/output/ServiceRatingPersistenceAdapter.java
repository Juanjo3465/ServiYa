package com.parosurvivors.serviya.feedback.infrastructure.adapters.output;

import com.parosurvivors.serviya.feedback.application.ports.output.ServiceRatingPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ServiceRating;
import com.parosurvivors.serviya.feedback.infrastructure.entities.ServiceRatingEntity;
import com.parosurvivors.serviya.feedback.infrastructure.mappers.ServiceRatingPersistenceMapper;
import com.parosurvivors.serviya.feedback.infrastructure.repositories.ServiceRatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ServiceRatingPersistenceAdapter implements ServiceRatingPersistencePort {

    private final ServiceRatingRepository repository;
    private final ServiceRatingPersistenceMapper mapper;

    @Override
    public ServiceRating save(ServiceRating rating) {
        ServiceRatingEntity saved = repository.save(mapper.toEntity(rating));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ServiceRating> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ServiceRating> findByRequestId(Long requestId) {
        return repository.findByRequestId(requestId).map(mapper::toDomain);
    }

    @Override
    public List<ServiceRating> findByClientId(Long clientId) {
        return repository.findByClientId(clientId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
