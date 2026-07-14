package com.parosurvivors.serviya.feedback.application.services;

import com.parosurvivors.serviya.feedback.application.dto.command.SubmitClientFeedbackCommand;
import com.parosurvivors.serviya.feedback.application.dto.result.ClientFeedbackResult;
import com.parosurvivors.serviya.feedback.application.mappers.ClientFeedbackCommandMapper;
import com.parosurvivors.serviya.feedback.application.ports.input.ClientFeedbackServicePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackTagCatalogPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackTagPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ClientFeedback;
import com.parosurvivors.serviya.feedback.domain.ClientFeedbackTagCatalog;
import com.parosurvivors.serviya.feedback.domain.TagSentiment;
import com.parosurvivors.serviya.requests.application.ports.output.ServiceRequestReadPort;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import com.parosurvivors.serviya.shared.events.domain.ClientFeedbackRevertedEvent;
import com.parosurvivors.serviya.shared.events.domain.ClientFeedbackSubmittedEvent;
import com.parosurvivors.serviya.shared.events.application.ports.output.DomainEventPublisherPort;
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
 * Fachada del feedback del oferente a un cliente (rating + reseña unificados en {@link ClientFeedback}).
 * Simétrico a {@code ServiceFeedbackService}: {@code submit}/{@code revert} persisten y publican
 * {@link ClientFeedbackSubmittedEvent}/{@link ClientFeedbackRevertedEvent} autocontenidos para las
 * métricas del cliente. Ver documents/project-structure/estructura-servicios.docx (módulo 5).
 */
@Component
@RequiredArgsConstructor
public class ClientFeedbackService implements ClientFeedbackServicePort {

    private final ClientFeedbackPersistencePort clientFeedbackPersistencePort;
    private final ClientFeedbackTagPersistencePort clientFeedbackTagPersistencePort;
    private final ClientFeedbackTagCatalogPersistencePort clientFeedbackTagCatalogPersistencePort;
    private final ServiceRequestReadPort serviceRequestReadPort;
    private final ClientFeedbackCommandMapper commandMapper;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public void submitClientFeedback(SubmitClientFeedbackCommand command) {
        ServiceRequest request = serviceRequestReadPort.findById(command.requestId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Solicitud no encontrada: " + command.requestId()));
        if (!request.getOffererId().equals(command.offererId())) {
            throw new UnauthorizedException("El feedback al cliente solo lo puede dejar el oferente de la solicitud");
        }
        if (!(request.isPresumablyCompleted() || request.isCompleted())) {
            throw new InvalidStateException(
                    "Solo se puede calificar una solicitud prestada; estado actual: " + request.getStatus());
        }
        if (clientFeedbackPersistencePort.findByRequestId(command.requestId()).isPresent()) {
            throw new InvalidStateException(
                    "Ya existe feedback de cliente para la solicitud " + command.requestId());
        }

        ClientFeedback feedback = commandMapper.toFeedback(command);
        feedback.setCreatedAt(LocalDateTime.now());
        ClientFeedback saved = clientFeedbackPersistencePort.save(feedback);

        List<Long> tagIds = command.tagIds() == null ? List.of() : command.tagIds();
        if (!tagIds.isEmpty()) {
            clientFeedbackTagPersistencePort.addTags(saved.getId(), tagIds);
        }

        eventPublisher.publish(new ClientFeedbackSubmittedEvent(
                request.getId(),
                saved.getClientId(),
                saved.getOffererId(),
                saved.getRating(),
                saved.hasComment(),
                resolveTags(tagIds)));
    }

    @Override
    @Transactional
    public boolean revertFeedback(Long requestId) {
        Optional<ClientFeedback> existing = clientFeedbackPersistencePort.findByRequestId(requestId);
        if (existing.isEmpty()) {
            return false;
        }
        ClientFeedback feedback = existing.get();
        List<Long> tagIds = clientFeedbackTagPersistencePort.findTagIdsByFeedbackId(feedback.getId());
        clientFeedbackTagPersistencePort.deleteByFeedbackId(feedback.getId());
        clientFeedbackPersistencePort.deleteById(feedback.getId());

        eventPublisher.publish(new ClientFeedbackRevertedEvent(
                requestId,
                feedback.getClientId(),
                feedback.getOffererId(),
                feedback.getRating(),
                feedback.hasComment(),
                resolveTags(tagIds)));
        return true;
    }

    @Override
    public ClientFeedbackResult getClientFeedback(Long requestId) {
        throw new UnsupportedOperationException("TODO: getClientFeedback — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Page<ClientFeedbackResult> getClientFeedbackList(Long clientId, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: getClientFeedbackList — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Page<ClientFeedbackResult> getClientFeedbackByOfferer(Long offererId, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: getClientFeedbackByOfferer — placeholder, ver estructura-servicios.docx");
    }

    /** Empareja cada tagId con su sentimiento (P/N) del catálogo para armar el payload autocontenido. */
    private List<TagRef> resolveTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        Map<Long, TagSentiment> sentimentByTag = clientFeedbackTagCatalogPersistencePort.findAll().stream()
                .collect(Collectors.toMap(ClientFeedbackTagCatalog::getId, ClientFeedbackTagCatalog::getSentiment));
        return tagIds.stream()
                .map(tagId -> new TagRef(tagId, sentimentByTag.get(tagId)))
                .toList();
    }
}
