package com.parosurvivors.serviya.feedback.application.ports.output;

import com.parosurvivors.serviya.feedback.domain.ServiceReviewTagCatalog;

import java.util.List;
import java.util.Optional;

public interface ServiceReviewTagCatalogPersistencePort {
    List<ServiceReviewTagCatalog> findAll();
    Optional<ServiceReviewTagCatalog> findById(Long id);
}
