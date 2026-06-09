package com.parosurvivors.serviya.feedback.infrastructure.adapters.output;

import com.parosurvivors.serviya.feedback.application.ports.output.ClientRatingPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ClientRating;
import com.parosurvivors.serviya.feedback.infrastructure.entities.ClientRatingEntity;
import com.parosurvivors.serviya.feedback.infrastructure.mappers.ClientRatingPersistenceMapper;
import com.parosurvivors.serviya.feedback.infrastructure.repositories.ClientRatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClientRatingPersistenceAdapter implements ClientRatingPersistencePort {

    private final ClientRatingRepository repository;
    private final ClientRatingPersistenceMapper mapper;

    @Override
    public ClientRating save(ClientRating rating) {
        ClientRatingEntity saved = repository.save(mapper.toEntity(rating));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ClientRating> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ClientRating> findByRequestId(Long requestId) {
        return repository.findByRequestId(requestId).map(mapper::toDomain);
    }

    @Override
    public List<ClientRating> findByClientId(Long clientId) {
        return repository.findByClientId(clientId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClientRating> findByOffererId(Long offererId) {
        return repository.findByOffererId(offererId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
