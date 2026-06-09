package com.parosurvivors.serviya.feedback.application.ports.output;

import com.parosurvivors.serviya.feedback.domain.ServiceRating;

import java.util.List;
import java.util.Optional;

public interface ServiceRatingPersistencePort {
    ServiceRating save(ServiceRating rating);
    Optional<ServiceRating> findById(Long id);
    Optional<ServiceRating> findByRequestId(Long requestId);
    List<ServiceRating> findByClientId(Long clientId);
    void deleteById(Long id);
}
