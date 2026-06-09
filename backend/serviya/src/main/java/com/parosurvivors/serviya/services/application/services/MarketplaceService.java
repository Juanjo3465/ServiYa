package com.parosurvivors.serviya.services.application.services;

import com.parosurvivors.serviya.services.application.dto.command.CreateServiceCommand;
import com.parosurvivors.serviya.services.application.dto.command.UpdateServiceCommand;
import com.parosurvivors.serviya.services.application.mappers.ServiceCommandMapper;
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

/**
 * Implementacion del marketplace de servicios. Unico servicio del esqueleto con logica real.
 * Construye el dominio a partir de los Commands (sin mapper web) y devuelve la entidad de dominio;
 * el controller mapea a Response via ServiceWebMapper.
 */
@Component
@RequiredArgsConstructor
public class MarketplaceService implements MarketplaceServicePort {

    private final ServicePersistencePort persistencePort;
    private final ServiceCommandMapper commandMapper;

    @Override
    public Service create(CreateServiceCommand command) {
        LocalDateTime now = LocalDateTime.now();
        Service service = commandMapper.toDomain(command);
        service.setCreatedAt(now);
        service.setUpdatedAt(now);
        return persistencePort.save(service);
    }

    @Override
    public Optional<Service> getById(Long id) {
        return persistencePort.findById(id)
                .filter(s -> !s.isDeleted());
    }

    @Override
    public List<Service> getAll() {
        return persistencePort.findAll().stream()
                .filter(s -> !s.isDeleted())
                .collect(Collectors.toList());
    }

    @Override
    public List<Service> getByOffererId(Long offererId) {
        return persistencePort.findByOffererId(offererId).stream()
                .filter(s -> !s.isDeleted())
                .collect(Collectors.toList());
    }

    @Override
    public Service update(UpdateServiceCommand command) {
        Service service = persistencePort.findById(command.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + command.serviceId()));

        // PATCH semantico: el mapper aplica solo los campos no-nulos del command (IGNORE strategy).
        commandMapper.updateFromCommand(command, service);
        service.setUpdatedAt(LocalDateTime.now());

        return persistencePort.update(service);
    }

    @Override
    public void delete(Long id) {
        if (persistencePort.findById(id).isEmpty()) {
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
