package com.parosurvivors.serviya.services.application.api;

import com.parosurvivors.serviya.services.api.ServiceCommandApi;
import com.parosurvivors.serviya.services.api.ServiceQueryApi;
import com.parosurvivors.serviya.services.api.dto.CategorySnapshot;
import com.parosurvivors.serviya.services.api.dto.ServiceAvailabilitySnapshot;
import com.parosurvivors.serviya.services.api.dto.ServiceSnapshot;
import com.parosurvivors.serviya.services.application.ports.input.MarketplaceServicePort;
import com.parosurvivors.serviya.services.application.ports.output.CategoryPersistencePort;
import com.parosurvivors.serviya.services.application.ports.output.ServiceAvailabilityPersistencePort;
import com.parosurvivors.serviya.services.application.ports.output.ServicePersistencePort;
import com.parosurvivors.serviya.services.domain.Category;
import com.parosurvivors.serviya.services.domain.Service;
import com.parosurvivors.serviya.services.domain.ServiceAvailability;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion de los contratos publicos de services sobre los casos de uso existentes.
 *
 * <p>Las lecturas usan los puertos de persistencia del propio modulo para no depender de
 * {@code MarketplaceService.getDetailById}, que compone perfiles/metricas/feedback. Las escrituras
 * delegan en {@link MarketplaceServicePort}.</p>
 */
@Component
@RequiredArgsConstructor
public class ServiceApiFacade implements ServiceQueryApi, ServiceCommandApi {

    private final ServicePersistencePort servicePersistencePort;
    private final CategoryPersistencePort categoryPersistencePort;
    private final ServiceAvailabilityPersistencePort serviceAvailabilityPersistencePort;
    private final MarketplaceServicePort marketplaceServicePort;

    // ---------------------------------------------------------------------
    // ServiceQueryApi
    // ---------------------------------------------------------------------

    @Override
    public Optional<ServiceSnapshot> findById(Long serviceId) {
        return servicePersistencePort.findById(serviceId).map(this::toServiceSnapshot);
    }

    @Override
    public List<ServiceSnapshot> findByIds(Collection<Long> serviceIds) {
        return servicePersistencePort.findByIds(serviceIds).stream()
                .map(this::toServiceSnapshot)
                .toList();
    }

    @Override
    public List<ServiceSnapshot> findByOffererId(Long offererId) {
        return servicePersistencePort.findByOffererId(offererId).stream()
                .map(this::toServiceSnapshot)
                .toList();
    }

    @Override
    public Optional<CategorySnapshot> findCategory(Long categoryId) {
        return categoryPersistencePort.findById(categoryId).map(this::toCategorySnapshot);
    }

    @Override
    public List<CategorySnapshot> getCategories() {
        return categoryPersistencePort.findAll().stream()
                .map(this::toCategorySnapshot)
                .toList();
    }

    @Override
    public List<ServiceAvailabilitySnapshot> getAvailabilities(Long serviceId) {
        return serviceAvailabilityPersistencePort.findByServiceId(serviceId).stream()
                .map(this::toAvailabilitySnapshot)
                .toList();
    }

    // ---------------------------------------------------------------------
    // ServiceCommandApi
    // ---------------------------------------------------------------------

    @Override
    public void deactivateAllByOfferer(Long offererId) {
        marketplaceServicePort.deactivateAllByOfferer(offererId);
    }

    @Override
    public void deleteServiceAsAdmin(Long serviceId, Long adminId) {
        marketplaceServicePort.delete(serviceId, adminId, true);
    }

    // ---------------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------------

    private ServiceSnapshot toServiceSnapshot(Service service) {
        return new ServiceSnapshot(
                service.getId(),
                service.getOffererId(),
                service.getTitle(),
                service.getDescription(),
                service.getPhotos(),
                service.getPriceHourly(),
                service.getCategoryId(),
                service.getAverageDurationMinutes(),
                Boolean.TRUE.equals(service.getActive()),
                service.getOperationRadiusKm(),
                service.getCreatedAt(),
                service.getUpdatedAt(),
                service.getDeletedAt());
    }

    private CategorySnapshot toCategorySnapshot(Category category) {
        return new CategorySnapshot(category.getId(), category.getName());
    }

    private ServiceAvailabilitySnapshot toAvailabilitySnapshot(ServiceAvailability availability) {
        return new ServiceAvailabilitySnapshot(
                availability.getId(),
                availability.getServiceId(),
                availability.getWeekDay(),
                availability.getStartTime(),
                availability.getEndTime(),
                availability.isActive());
    }
}
