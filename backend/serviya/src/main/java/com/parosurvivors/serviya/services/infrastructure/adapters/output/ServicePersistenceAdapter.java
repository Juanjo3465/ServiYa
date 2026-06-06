package com.parosurvivors.serviya.services.infrastructure.adapters.output;

import com.parosurvivors.serviya.services.application.ports.output.ServicePersistencePort;
import com.parosurvivors.serviya.services.domain.Service;
import com.parosurvivors.serviya.services.infrastructure.entities.ServiceEntity;
import com.parosurvivors.serviya.services.infrastructure.repositories.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ServicePersistenceAdapter implements ServicePersistencePort {
    
    private final ServiceRepository repository;

    @Override
    public Service save(Service service) {
        ServiceEntity entity = toDomainEntity(service);
        ServiceEntity saved = repository.save(entity);
        return toDomainModel(saved);
    }

    @Override
    public Optional<Service> findById(Long id) {
        return repository.findById(id)
                .map(this::toDomainModel);
    }

    @Override
    public List<Service> findAll() {
        return repository.findAll().stream()
                .map(this::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Service> findByOffererId(Long offererId) {
        return repository.findByOffererId(offererId).stream()
                .map(this::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Service update(Service service) {
        ServiceEntity entity = toDomainEntity(service);
        ServiceEntity updated = repository.save(entity);
        return toDomainModel(updated);
    }

    private Service toDomainModel(ServiceEntity entity) {
        return Service.builder()
                .id(entity.getId())
                .offererId(entity.getOffererId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .photos(entity.getPhotos())
                .priceHourly(entity.getPriceHourly())
                .categoryId(entity.getCategoryId())
                .averageDurationMinutes(entity.getAverageDurationMinutes())
                .active(entity.getActive())
                .operationRadiusKm(entity.getOperationRadiusKm())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    private ServiceEntity toDomainEntity(Service domain) {
        ServiceEntity entity = new ServiceEntity();
        entity.setId(domain.getId());
        entity.setOffererId(domain.getOffererId());
        entity.setTitle(domain.getTitle());
        entity.setDescription(domain.getDescription());
        entity.setPhotos(domain.getPhotos());
        entity.setPriceHourly(domain.getPriceHourly());
        entity.setCategoryId(domain.getCategoryId());
        entity.setAverageDurationMinutes(domain.getAverageDurationMinutes());
        entity.setActive(domain.getActive());
        entity.setOperationRadiusKm(domain.getOperationRadiusKm());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setDeletedAt(domain.getDeletedAt());
        return entity;
    }
}
