package com.parosurvivors.serviya.feedback.application.ports.output;

import com.parosurvivors.serviya.feedback.domain.ClientReviewTagCatalog;

import java.util.List;
import java.util.Optional;

public interface ClientReviewTagCatalogPersistencePort {
    List<ClientReviewTagCatalog> findAll();
    Optional<ClientReviewTagCatalog> findById(Long id);
}
