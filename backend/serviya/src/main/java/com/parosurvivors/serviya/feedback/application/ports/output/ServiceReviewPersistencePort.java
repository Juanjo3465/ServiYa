package com.parosurvivors.serviya.feedback.application.ports.output;

import com.parosurvivors.serviya.feedback.domain.ServiceReview;

import java.util.List;
import java.util.Optional;

public interface ServiceReviewPersistencePort {
    ServiceReview save(ServiceReview review);
    Optional<ServiceReview> findById(Long id);
    Optional<ServiceReview> findByRequestId(Long requestId);
    List<ServiceReview> findByClientId(Long clientId);
    void deleteById(Long id);
}
