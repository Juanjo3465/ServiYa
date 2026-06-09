package com.parosurvivors.serviya.feedback.infrastructure.adapters.output;

import com.parosurvivors.serviya.feedback.application.ports.output.ClientReviewPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ClientReview;
import com.parosurvivors.serviya.feedback.infrastructure.entities.ClientReviewEntity;
import com.parosurvivors.serviya.feedback.infrastructure.mappers.ClientReviewPersistenceMapper;
import com.parosurvivors.serviya.feedback.infrastructure.repositories.ClientReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClientReviewPersistenceAdapter implements ClientReviewPersistencePort {

    private final ClientReviewRepository repository;
    private final ClientReviewPersistenceMapper mapper;

    @Override
    public ClientReview save(ClientReview review) {
        ClientReviewEntity saved = repository.save(mapper.toEntity(review));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ClientReview> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ClientReview> findByRequestId(Long requestId) {
        return repository.findByRequestId(requestId).map(mapper::toDomain);
    }

    @Override
    public List<ClientReview> findByOffererId(Long offererId) {
        return repository.findByOffererId(offererId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
