package com.parosurvivors.serviya.services.application.services;

import com.parosurvivors.serviya.feedback.application.ports.input.ServiceFeedbackServicePort;
import com.parosurvivors.serviya.metrics.application.ports.input.OffererMetricsServicePort;
import com.parosurvivors.serviya.metrics.domain.OffererMetrics;
import com.parosurvivors.serviya.profiles.application.ports.input.OffererProfileServicePort;
import com.parosurvivors.serviya.profiles.application.ports.input.UserProfileServicePort;
import com.parosurvivors.serviya.profiles.domain.OffererProfile;
import com.parosurvivors.serviya.profiles.domain.OffererProfileSummary;
import com.parosurvivors.serviya.profiles.domain.UserProfile;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final MarketplaceCategoryPort categoryPort;
    private final OffererProfileServicePort offererProfileService;
    private final OffererMetricsServicePort offererMetricsService;
    private final ServiceFeedbackServicePort serviceFeedbackService;
    private final UserProfileServicePort userProfileService;
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
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada con id: " + service.getCategoryId()));
            details.add(new ServiceDetail(service, category, null, null, null, null));
        }

        return details;
    }


    @Override
    public Page<Service> search(SearchServiceQuery criteria, Pageable pageable) {
        return persistencePort.search(criteria, pageable);
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
    public Optional<ServiceDetail> getDetailById(Long id) {
        
        Service service = getById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));

        Category category = categoryPort.getById(service.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada con id: " + service.getCategoryId()));

        OffererProfile offererProfile = offererProfileService.getPublicProfile(service.getOffererId());
        OffererProfileSummary summary = offererProfileService.getProfileSummary(service.getOffererId());
        OffererMetrics metrics = offererMetricsService.getMainMetrics(service.getOffererId());

        // Reseñas recientes (hasta 3): cada feedback con comentario se empareja con el perfil
        // público de su autor para mostrar nombre y foto en el detalle del servicio.
        List<FeedbackUser> feedbacks = serviceFeedbackService.getRecentServiceFeedback(service.getId(), 3).stream()
                .map(feedback -> new FeedbackUser(feedback, userProfileService.getProfileInfo(feedback.getClientId())))
                .collect(Collectors.toList());

        List<ServiceAvailability> availability = serviceAvailabilityService.getByServiceId(service.getId());

        return Optional.of(new ServiceDetail(service, category, offererProfile, summary, feedbacks, availability));
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
