package com.parosurvivors.serviya.services.application.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.parosurvivors.serviya.services.application.dto.command.CreateServiceAvailabilityCommand;
import com.parosurvivors.serviya.services.application.dto.command.UpdateServiceAvailabilityCommand;
import com.parosurvivors.serviya.services.application.ports.input.ServiceAvailabilityServicePort;
import com.parosurvivors.serviya.services.application.ports.output.ServiceAvailabilityPersistencePort;
import com.parosurvivors.serviya.services.domain.ServiceAvailability;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor

public class ServiceAvailabilityService implements ServiceAvailabilityServicePort {

    private final ServiceAvailabilityPersistencePort persistencePort;

    @Override
    public ServiceAvailability create(CreateServiceAvailabilityCommand command) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'create'");
    }

    @Override
    public void delete(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public ServiceAvailability update(UpdateServiceAvailabilityCommand command) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

    @Override
    public List<ServiceAvailability> getByServiceId(long serviceId) {
        return persistencePort.findByServiceId(serviceId).stream()
                .filter(a -> !a.isActive())
                .collect(Collectors.toList());
    }
    
    
    
}
