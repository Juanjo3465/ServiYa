package com.parosurvivors.serviya.requests.infrastructure.adapters.output;

import com.parosurvivors.serviya.requests.application.ports.output.ServiceRequestPersistencePort;
import com.parosurvivors.serviya.requests.domain.RequestStatus;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import com.parosurvivors.serviya.requests.infrastructure.entities.ServiceRequestEntity;
import com.parosurvivors.serviya.requests.infrastructure.mappers.ServiceRequestPersistenceMapper;
import com.parosurvivors.serviya.requests.infrastructure.repositories.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ServiceRequestPersistenceAdapter implements ServiceRequestPersistencePort {

    private final ServiceRequestRepository repository;
    private final ServiceRequestPersistenceMapper mapper;

    @Override
    public ServiceRequest save(ServiceRequest request) {
        ServiceRequestEntity saved = repository.save(mapper.toEntity(request));
        return mapper.toDomain(saved);
    }

    @Override
    public ServiceRequest update(ServiceRequest request) {
        ServiceRequestEntity updated = repository.save(mapper.toEntity(request));
        return mapper.toDomain(updated);
    }

    @Override
    public Optional<ServiceRequest> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ServiceRequest> findByClientId(Long clientId) {
        return repository.findByClientId(clientId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceRequest> findByOffererId(Long offererId) {
        return repository.findByOffererId(offererId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceRequest> findByServiceId(Long serviceId) {
        return repository.findByServiceId(serviceId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceRequest> findByStatus(RequestStatus status) {
        return repository.findByStatus(status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ServiceRequest> findByPreviousRequestId(Long previousRequestId) {
        return repository.findByPreviousRequestId(previousRequestId).map(mapper::toDomain);
    }

    @Override
    public long countByClientId(Long clientId) {
        return repository.countByClientId(clientId);
    }

    @Override
    public long countByOffererId(Long offererId) {
        return repository.countByOffererId(offererId);
    }
}
