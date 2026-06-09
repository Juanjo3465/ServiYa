package com.parosurvivors.serviya.feedback.application.ports.output;

import com.parosurvivors.serviya.feedback.domain.ClientReview;

import java.util.List;
import java.util.Optional;

public interface ClientReviewPersistencePort {
    ClientReview save(ClientReview review);
    Optional<ClientReview> findById(Long id);
    Optional<ClientReview> findByRequestId(Long requestId);
    List<ClientReview> findByOffererId(Long offererId);
    void deleteById(Long id);
}
