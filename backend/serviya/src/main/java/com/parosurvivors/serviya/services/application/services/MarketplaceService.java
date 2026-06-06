package com.parosurvivors.serviya.services.application.services;

import com.parosurvivors.serviya.services.application.dto.ServiceRequest;
import com.parosurvivors.serviya.services.application.dto.ServiceResponse;
import com.parosurvivors.serviya.services.application.dto.ServiceSearchCriteria;
import com.parosurvivors.serviya.services.application.mappers.ServiceMapper;
import com.parosurvivors.serviya.services.application.ports.input.MarketplaceServicePort;
import com.parosurvivors.serviya.services.application.ports.output.ServicePersistencePort;
import com.parosurvivors.serviya.services.domain.Service;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MarketplaceService implements MarketplaceServicePort {
    
    private final ServicePersistencePort persistencePort;
    private final ServiceMapper mapper;

    @Override
    public ServiceResponse create(ServiceRequest request) {
        Service service = mapper.toDomain(request);
        service.setCreatedAt(LocalDateTime.now());
        service.setUpdatedAt(LocalDateTime.now());
        
        Service saved = persistencePort.save(service);
        return mapper.toResponse(saved);
    }

    @Override
    public Optional<ServiceResponse> getById(Long id) {
        return persistencePort.findById(id)
                .filter(s -> !s.isDeleted())
                .map(mapper::toResponse);
    }

    @Override
    public List<ServiceResponse> getAll() {
        return persistencePort.findAll().stream()
                .filter(s -> !s.isDeleted())
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceResponse> getByOffererId(Long offererId) {
        return persistencePort.findByOffererId(offererId).stream()
                .filter(s -> !s.isDeleted())
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceResponse> search(ServiceSearchCriteria criteria) {
        return persistencePort.search(criteria).stream()
                .filter(s -> !s.isDeleted())
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }
    @Override
    public ServiceResponse update(Long id, ServiceRequest request) {
        Service service = persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));
        
        mapper.updateFromRequest(request, service);
        service.setUpdatedAt(LocalDateTime.now());
        
        Service updated = persistencePort.update(service);
        return mapper.toResponse(updated);
    }

    @Override
    public void delete(Long id) {
        if (!persistencePort.findById(id).isPresent()) {
            throw new ResourceNotFoundException("Servicio no encontrado con id: " + id);
        }
        persistencePort.deleteById(id);
    }

    @Override
    public void softDelete(Long id) {
        Service service = persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));
        
        service.softDelete();
        service.setUpdatedAt(LocalDateTime.now());
        persistencePort.update(service);
    }

    @Override
    public void activate(Long id) {
        Service service = persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));
        
        service.activate();
        service.setUpdatedAt(LocalDateTime.now());
        persistencePort.update(service);
    }

    @Override
    public void deactivate(Long id) {
        Service service = persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));
        
        service.deactivate();
        service.setUpdatedAt(LocalDateTime.now());
        persistencePort.update(service);
    }
}
