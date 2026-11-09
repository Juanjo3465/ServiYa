package com.parosurvivors.serviya.services.application.services;

import com.parosurvivors.serviya.feedback.api.FeedbackQueryApi;
import com.parosurvivors.serviya.metrics.api.MetricsQueryApi;
import com.parosurvivors.serviya.metrics.api.dto.OffererMetricsSnapshot;
import com.parosurvivors.serviya.metrics.api.dto.ServiceMetricsSnapshot;
import com.parosurvivors.serviya.profiles.api.ProfileQueryApi;
import com.parosurvivors.serviya.profiles.api.dto.OffererProfileSnapshot;
import com.parosurvivors.serviya.profiles.api.dto.OffererProfileSummarySnapshot;
import com.parosurvivors.serviya.profiles.api.dto.ProfileSnapshot;
import com.parosurvivors.serviya.services.application.dto.command.CreateServiceCommand;
import com.parosurvivors.serviya.services.application.dto.command.UpdateServiceCommand;
import com.parosurvivors.serviya.services.application.dto.query.SearchServiceQuery;
import com.parosurvivors.serviya.services.application.mappers.ServiceCommandMapper;
import com.parosurvivors.serviya.services.application.ports.input.MarketplaceServicePort;
import com.parosurvivors.serviya.services.application.ports.input.ServiceAvailabilityServicePort;
import com.parosurvivors.serviya.services.application.ports.output.ServicePersistencePort;
import com.parosurvivors.serviya.services.application.ports.input.MarketplaceCategoryPort;
import com.parosurvivors.serviya.services.domain.Service;
import com.parosurvivors.serviya.services.domain.Category;
import com.parosurvivors.serviya.services.domain.ServiceDetail;
import com.parosurvivors.serviya.services.domain.FeedbackUser;
import com.parosurvivors.serviya.services.domain.ServiceAvailability;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import com.parosurvivors.serviya.shared.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementacion del marketplace de servicios. Unico servicio del esqueleto con
 * logica real.
 * Construye el dominio a partir de los Commands (sin mapper web) y devuelve la
 * entidad de dominio;
 * el controller mapea a Response via ServiceWebMapper.
 */
@Component
@RequiredArgsConstructor
public class MarketplaceService implements MarketplaceServicePort {

    private final ServicePersistencePort persistencePort;
    private final MarketplaceCategoryPort categoryPort;
    private final ProfileQueryApi profileQueryApi;
    private final MetricsQueryApi metricsQueryApi;
    private final FeedbackQueryApi feedbackQueryApi;
    private final ServiceAvailabilityServicePort serviceAvailabilityService;
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
    public List<ServiceDetail> getByOffererId(Long offererId) {
        List<Service> services = persistencePort.findByOffererId(offererId);

        if (services.isEmpty())
            throw new ResourceNotFoundException("El oferente no tiene servicios");

        List<ServiceDetail> details = new ArrayList<>();

        for (Service service : services) {
            Category category = categoryPort.getById(service.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Categoria no encontrada con id: " + service.getCategoryId()));
            details.add(ServiceDetail.builder()
                    .service(service)
                    .category(category)
                    .build());
        }

        return details;
    }

    @Override
    public Page<Service> search(SearchServiceQuery criteria, Pageable pageable) {
        SearchServiceQuery effective = criteria;
        if (criteria.minRating() != null || criteria.maxRating() != null) {
            List<Long> ids = metricsQueryApi.findServiceIdsByAverageRating(
                    criteria.minRating(), criteria.maxRating());
            if (ids.isEmpty()) {
                return Page.empty(pageable);
            }
            effective = criteria.withServiceIds(ids);
        }
        return persistencePort.search(effective, pageable);
    }

    @Override
    public Map<Long, ServiceMetricsSnapshot> getMetricsForServices(List<Long> serviceIds) {
        return metricsQueryApi.getServiceMetrics(serviceIds);
    }

    @Override
    public Service update(UpdateServiceCommand command, Long requesterId, boolean isAdmin) {
        Service service = persistencePort.findById(command.serviceId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Servicio no encontrado con id: " + command.serviceId()));

        requireOwnership(service, requesterId, isAdmin);

        // PATCH semantico: el mapper aplica solo los campos no-nulos del command
        // (IGNORE strategy).
        commandMapper.updateFromCommand(command, service);
        service.setUpdatedAt(LocalDateTime.now());

        return persistencePort.update(service);
    }

    @Override
    public Optional<ServiceDetail> getDetailById(Long id) {

        Service service = getById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));

        Category category = categoryPort.getById(service.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoria no encontrada con id: " + service.getCategoryId()));

        OffererProfileSnapshot offererProfile = profileQueryApi.findOffererProfile(service.getOffererId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de oferente no encontrado para userId: " + service.getOffererId()));
        OffererProfileSummarySnapshot summary = profileQueryApi.getOffererSummary(service.getOffererId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resumen de oferente no encontrado para userId: " + service.getOffererId()));
        OffererMetricsSnapshot offererMetrics = metricsQueryApi.getOffererMainMetrics(service.getOffererId());
        ServiceMetricsSnapshot serviceMetrics = metricsQueryApi.getServiceMetrics(service.getId());

        // Reseñas recientes (hasta 3): cada feedback con comentario se empareja con el
        // perfil público de su autor para mostrar nombre y foto en el detalle del servicio.
        List<FeedbackUser> feedbacks = feedbackQueryApi.getRecentServiceFeedback(service.getId(), 3).stream()
                .map(feedback -> new FeedbackUser(
                        feedback,
                        profileQueryApi.findProfile(feedback.clientId()).orElse(null)))
                .collect(Collectors.toList());

        List<ServiceAvailability> availability = serviceAvailabilityService.getByServiceId(service.getId());

        return Optional.of(ServiceDetail.builder()
                .service(service)
                .category(category)
                .offererProfile(offererProfile)
                .offererSummary(summary)
                .feedbackUsers(feedbacks)
                .availability(availability)
                .serviceMetrics(serviceMetrics)
                .offererMetrics(offererMetrics)
                .build());
    }

    @Override
    public void delete(Long id, Long requesterId, boolean isAdmin) {
        Service service = persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));
        requireOwnership(service, requesterId, isAdmin);
        persistencePort.deleteById(id);
    }

    @Override
    public void softDelete(Long id, Long requesterId, boolean isAdmin) {
        Service service = persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));

        requireOwnership(service, requesterId, isAdmin);
        service.softDelete();
        service.setUpdatedAt(LocalDateTime.now());
        persistencePort.update(service);
    }

    @Override
    public void activate(Long id, Long requesterId, boolean isAdmin) {
        Service service = persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));

        requireOwnership(service, requesterId, isAdmin);
        service.activate();
        service.setUpdatedAt(LocalDateTime.now());
        persistencePort.update(service);
    }

    @Override
    public void deactivate(Long id, Long requesterId, boolean isAdmin) {
        Service service = persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));

        requireOwnership(service, requesterId, isAdmin);
        service.deactivate();
        service.setUpdatedAt(LocalDateTime.now());
        persistencePort.update(service);
    }

    /**
     * El actor debe ser el oferente propietario del servicio, o un admin (IDOR: B1).
     * Mismo patron "participante O admin" que {@code ServiceRequestQueryService.getRequestHistory}.
     */
    private void requireOwnership(Service service, Long requesterId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (requesterId == null || !requesterId.equals(service.getOffererId())) {
            throw new UnauthorizedException("El usuario no es el propietario del servicio");
        }
    }

    @Override
    public void deactivateAllByOfferer(Long offererId) {
        // Usado por UserDeletionService al eliminar un oferente: oculta todos sus servicios del buscador.
        // Corre dentro de la transacción del orquestador llamante.
        LocalDateTime now = LocalDateTime.now();
        for (Service service : persistencePort.findByOffererId(offererId)) {
            if (service.isAvailable()) {
                service.deactivate();
                service.setUpdatedAt(now);
                persistencePort.update(service);
            }
        }
    }

}
