package com.parosurvivors.serviya.feedback.application.ports.output;

import com.parosurvivors.serviya.feedback.domain.ClientRating;

import java.util.List;
import java.util.Optional;

public interface ClientRatingPersistencePort {
    ClientRating save(ClientRating rating);
    Optional<ClientRating> findById(Long id);
    Optional<ClientRating> findByRequestId(Long requestId);
    List<ClientRating> findByClientId(Long clientId);
    List<ClientRating> findByOffererId(Long offererId);
    void deleteById(Long id);
}
