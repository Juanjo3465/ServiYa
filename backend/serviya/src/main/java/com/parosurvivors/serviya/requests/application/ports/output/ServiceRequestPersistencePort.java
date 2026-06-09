package com.parosurvivors.serviya.requests.application.ports.output;

import com.parosurvivors.serviya.requests.domain.RequestStatus;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;

import java.util.List;
import java.util.Optional;

public interface ServiceRequestPersistencePort {
    ServiceRequest save(ServiceRequest request);
    ServiceRequest update(ServiceRequest request);
    Optional<ServiceRequest> findById(Long id);
    List<ServiceRequest> findByClientId(Long clientId);
    List<ServiceRequest> findByOffererId(Long offererId);
    List<ServiceRequest> findByServiceId(Long serviceId);
    List<ServiceRequest> findByStatus(RequestStatus status);
    Optional<ServiceRequest> findByPreviousRequestId(Long previousRequestId);
    long countByClientId(Long clientId);
    long countByOffererId(Long offererId);
}
