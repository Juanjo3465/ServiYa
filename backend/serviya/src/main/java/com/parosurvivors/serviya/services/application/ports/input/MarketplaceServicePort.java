package com.parosurvivors.serviya.services.application.ports.input;

import com.parosurvivors.serviya.services.application.dto.ServiceRequest;
import com.parosurvivors.serviya.services.application.dto.ServiceResponse;
import com.parosurvivors.serviya.services.application.dto.ServiceSearchCriteria;

import java.util.List;
import java.util.Optional;

public interface MarketplaceServicePort {
    ServiceResponse create(ServiceRequest request);
    Optional<ServiceResponse> getById(Long id);
    List<ServiceResponse> getAll();
    List<ServiceResponse> getByOffererId(Long offererId);
    List<ServiceResponse> search(ServiceSearchCriteria criteria);
    ServiceResponse update(Long id, ServiceRequest request);
    void delete(Long id);
    void softDelete(Long id);
    void activate(Long id);
    void deactivate(Long id);
}
