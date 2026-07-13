package com.parosurvivors.serviya.feedback.application.services;

import com.parosurvivors.serviya.feedback.application.dto.command.SubmitServiceFeedbackCommand;
import com.parosurvivors.serviya.feedback.application.dto.result.ServiceFeedbackResult;
import com.parosurvivors.serviya.feedback.application.mappers.ServiceFeedbackCommandMapper;
import com.parosurvivors.serviya.feedback.application.ports.input.ServiceFeedbackServicePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackTagCatalogPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackTagPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ServiceFeedback;
import com.parosurvivors.serviya.feedback.domain.ServiceFeedbackTagCatalog;
import com.parosurvivors.serviya.feedback.domain.TagSentiment;
import com.parosurvivors.serviya.requests.application.ports.output.ServiceRequestReadPort;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import com.parosurvivors.serviya.shared.events.application.ports.output.DomainEventPublisherPort;
import com.parosurvivors.serviya.shared.events.domain.ServiceFeedbackRevertedEvent;
import com.parosurvivors.serviya.shared.events.domain.ServiceFeedbackSubmittedEvent;
import com.parosurvivors.serviya.shared.events.domain.TagRef;
import com.parosurvivors.serviya.shared.exceptions.InvalidStateException;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import com.parosurvivors.serviya.shared.exceptions.UnauthorizedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fachada del feedback del cliente a un servicio (rating + reseña unificados en {@link ServiceFeedback}).
 * {@code submit}/{@code revert} son los publicadores: persisten el feedback + sus tags y publican un
 * {@link ServiceFeedbackSubmittedEvent}/{@link ServiceFeedbackRevertedEvent} autocontenido (con el
 * sentimiento de cada tag ya resuelto desde el catálogo) para que las métricas se recalculen por evento.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 5).
 */
@Component
@RequiredArgsConstructor
public class ServiceFeedbackService implements ServiceFeedbackServicePort {

    private final ServiceFeedbackPersistencePort serviceFeedbackPersistencePort;
    private final ServiceFeedbackTagPersistencePort serviceFeedbackTagPersistencePort;
    private final ServiceFeedbackTagCatalogPersistencePort serviceFeedbackTagCatalogPersistencePort;
    private final ServiceRequestReadPort serviceRequestReadPort;
    private final ServiceFeedbackCommandMapper commandMapper;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public void submitServiceFeedback(SubmitServiceFeedbackCommand command) {
        ServiceRequest request = serviceRequestReadPort.findById(command.requestId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Solicitud no encontrada: " + command.requestId()));
        if (!request.getClientId().equals(command.clientId())) {
            throw new UnauthorizedException("El feedback solo lo puede dejar el cliente de la solicitud");
        }
        // Ventana de calificación: desde 'presuntamente prestada' en adelante (no solo 'completada').
        if (!(request.isPresumablyCompleted() || request.isCompleted())) {
            throw new InvalidStateException(
                    "Solo se puede calificar una solicitud prestada; estado actual: " + request.getStatus());
        }
        if (serviceFeedbackPersistencePort.findByRequestId(command.requestId()).isPresent()) {
            throw new InvalidStateException(
                    "Ya existe feedback para la solicitud " + command.requestId());
        }

        ServiceFeedback feedback = commandMapper.toFeedback(command);
        feedback.setServiceId(request.getServiceId());
        feedback.setCreatedAt(LocalDateTime.now());
        ServiceFeedback saved = serviceFeedbackPersistencePort.save(feedback);

        List<Long> tagIds = command.tagIds() == null ? List.of() : command.tagIds();
        if (!tagIds.isEmpty()) {
            serviceFeedbackTagPersistencePort.addTags(saved.getId(), tagIds);
        }

        eventPublisher.publish(new ServiceFeedbackSubmittedEvent(
                request.getId(),
                request.getServiceId(),
                request.getOffererId(),
                request.getClientId(),
                saved.getRating(),
                saved.hasComment(),
                resolveTags(tagIds)));
    }

    @Override
    @Transactional
    public boolean revertFeedback(Long requestId) {
        Optional<ServiceFeedback> existing = serviceFeedbackPersistencePort.findByRequestId(requestId);
        if (existing.isEmpty()) {
            return false;
        }
        ServiceFeedback feedback = existing.get();
        List<Long> tagIds = serviceFeedbackTagPersistencePort.findTagIdsByFeedbackId(feedback.getId());
        serviceFeedbackTagPersistencePort.deleteByFeedbackId(feedback.getId());
        serviceFeedbackPersistencePort.deleteById(feedback.getId());

        // El oferente objetivo no vive en el feedback; se lee de la solicitud para el evento de reverso.
        Long offererId = serviceRequestReadPort.findById(requestId)
                .map(ServiceRequest::getOffererId)
                .orElse(null);

        eventPublisher.publish(new ServiceFeedbackRevertedEvent(
                requestId,
                feedback.getServiceId(),
                offererId,
                feedback.getClientId(),
                feedback.getRating(),
                feedback.hasComment(),
                resolveTags(tagIds)));
        return true;
    }

    @Override
    public ServiceFeedbackResult getServiceFeedback(Long requestId) {
        throw new UnsupportedOperationException("TODO: getServiceFeedback — placeholder, ver estructura-servicios.docx");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ServiceFeedbackResult> getServiceFeedbackById(Long feedbackId) {
        return serviceFeedbackPersistencePort.findById(feedbackId).map(f -> new ServiceFeedbackResult(
                f.getRequestId(),
                f.getServiceId(),
                f.getClientId(),
                f.getRating(),
                f.getComment(),
                resolveTagNames(serviceFeedbackTagPersistencePort.findTagIdsByFeedbackId(f.getId())),
                f.getCreatedAt()));
    }

    @Override
    public Page<ServiceFeedbackResult> getServiceFeedbackList(Long serviceId, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: getServiceFeedbackList — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Page<ServiceFeedbackResult> getServiceFeedbackByClient(Long clientId, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: getServiceFeedbackByClient — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public List<ServiceFeedback> getRecentServiceFeedback(Long serviceId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return serviceFeedbackPersistencePort.findRecentByServiceId(serviceId, limit);
    }

    /** Resuelve los nombres de las etiquetas desde el catálogo (para vistas de lectura como el detalle de reporte). */
    private List<String> resolveTagNames(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        Map<Long, String> nameById = serviceFeedbackTagCatalogPersistencePort.findAll().stream()
                .collect(Collectors.toMap(ServiceFeedbackTagCatalog::getId, ServiceFeedbackTagCatalog::getTagName));
        return tagIds.stream()
                .map(nameById::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /** Empareja cada tagId con su sentimiento (P/N) del catálogo para armar el payload autocontenido. */
    private List<TagRef> resolveTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        Map<Long, TagSentiment> sentimentByTag = serviceFeedbackTagCatalogPersistencePort.findAll().stream()
                .collect(Collectors.toMap(ServiceFeedbackTagCatalog::getId, ServiceFeedbackTagCatalog::getSentiment));
        return tagIds.stream()
                .map(tagId -> new TagRef(tagId, sentimentByTag.get(tagId)))
                .toList();
    }
}
