package com.parosurvivors.serviya.services.application.ports.output;

import com.parosurvivors.serviya.services.domain.Service;

import java.util.List;
import java.util.Optional;

public interface ServicePersistencePort {
    Service save(Service service);
    Optional<Service> findById(Long id);
    List<Service> findAll();
    List<Service> findByOffererId(Long offererId);
    List<Service> search(com.parosurvivors.serviya.services.application.dto.query.SearchServiceQuery criteria);
    void deleteById(Long id);
    Service update(Service service);
}
