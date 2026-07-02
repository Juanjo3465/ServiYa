package com.parosurvivors.serviya.feedback.application.services;

import com.parosurvivors.serviya.feedback.application.dto.FeedbackParts;
import com.parosurvivors.serviya.feedback.application.dto.FeedbackSide;
import com.parosurvivors.serviya.feedback.application.events.ClientRatedEvent;
import com.parosurvivors.serviya.feedback.application.events.FeedbackTagEventItem;
import com.parosurvivors.serviya.feedback.application.events.ReviewCreatedEvent;
import com.parosurvivors.serviya.feedback.application.events.ReviewDeletedEvent;
import com.parosurvivors.serviya.feedback.application.events.ServiceRatedEvent;
import com.parosurvivors.serviya.feedback.application.ports.input.FeedbackFlowPort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackTagCatalogPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackTagPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackTagCatalogPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackTagPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ClientFeedback;
import com.parosurvivors.serviya.feedback.domain.ClientFeedbackTagCatalog;
import com.parosurvivors.serviya.feedback.domain.ServiceFeedback;
import com.parosurvivors.serviya.feedback.domain.ServiceFeedbackTagCatalog;
import com.parosurvivors.serviya.requests.application.ports.output.ServiceRequestPersistencePort;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import com.parosurvivors.serviya.shared.exceptions.InvalidStateException;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Flujo compartido de creación y borrado de feedback (rating + reseña unificados).
 * Valida el estado de la solicitud, persiste y publica eventos para métricas.
 */
@Component
@RequiredArgsConstructor
public class FeedbackFlow implements FeedbackFlowPort {

    private final ServiceRequestPersistencePort serviceRequestPersistencePort;
    private final ServiceFeedbackPersistencePort serviceFeedbackPersistencePort;
    private final ServiceFeedbackTagPersistencePort serviceFeedbackTagPersistencePort;
    private final ServiceFeedbackTagCatalogPersistencePort serviceFeedbackTagCatalogPersistencePort;
    private final ClientFeedbackPersistencePort clientFeedbackPersistencePort;
    private final ClientFeedbackTagPersistencePort clientFeedbackTagPersistencePort;
    private final ClientFeedbackTagCatalogPersistencePort clientFeedbackTagCatalogPersistencePort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void submit(FeedbackParts parts, Long requestId, Integer rating, String comment, List<Long> tagIds) {
        ServiceRequest request = loadRequest(requestId);
        requireFeedbackEligible(request);

        String normalizedComment = normalizeComment(comment);
        List<Long> safeTagIds = tagIds == null ? List.of() : tagIds;

        if (rating == null && normalizedComment == null && safeTagIds.isEmpty()) {
            return;
        }

        validateRating(rating);
        List<FeedbackTagEventItem> tagItems = resolveTagItems(parts.side(), safeTagIds);

        if (parts.side() == FeedbackSide.SERVICE) {
            submitServiceFeedback(request, rating, normalizedComment, safeTagIds, tagItems);
        } else {
            submitClientFeedback(request, rating, normalizedComment, safeTagIds, tagItems);
        }
    }

    @Override
    @Transactional
    public void remove(FeedbackParts parts, Long requestId) {
        if (parts.side() == FeedbackSide.SERVICE) {
            removeServiceFeedback(requestId);
        } else {
            removeClientFeedback(requestId);
        }
    }

    private void submitServiceFeedback(ServiceRequest request, Integer rating, String comment,
                                       List<Long> tagIds, List<FeedbackTagEventItem> tagItems) {
        ServiceFeedback existing = serviceFeedbackPersistencePort.findByRequestId(request.getId()).orElse(null);
        Integer previousRating = existing != null ? existing.getRating() : null;
        boolean hadComment = existing != null && existing.hasComment();
        List<FeedbackTagEventItem> previousTags = existing != null
                ? resolveExistingServiceTagItems(existing.getId())
                : List.of();

        ServiceFeedback feedback = existing != null ? existing : new ServiceFeedback();
        feedback.setRequestId(request.getId());
        feedback.setClientId(request.getClientId());
        feedback.setServiceId(request.getServiceId());
        feedback.setRating(rating);
        feedback.setComment(comment);
        feedback.setTagIds(tagIds);

        ServiceFeedback saved = serviceFeedbackPersistencePort.save(feedback);
        replaceServiceTags(saved.getId(), tagIds);

        publishRatingEvents(FeedbackSide.SERVICE, request, previousRating, rating);
        publishReviewEvents(FeedbackSide.SERVICE, request, hadComment, comment != null, previousTags, tagItems);
    }

    private void submitClientFeedback(ServiceRequest request, Integer rating, String comment,
                                      List<Long> tagIds, List<FeedbackTagEventItem> tagItems) {
        ClientFeedback existing = clientFeedbackPersistencePort.findByRequestId(request.getId()).orElse(null);
        Integer previousRating = existing != null ? existing.getRating() : null;
        boolean hadComment = existing != null && existing.hasComment();
        List<FeedbackTagEventItem> previousTags = existing != null
                ? resolveExistingClientTagItems(existing.getId())
                : List.of();

        ClientFeedback feedback = existing != null ? existing : new ClientFeedback();
        feedback.setRequestId(request.getId());
        feedback.setOffererId(request.getOffererId());
        feedback.setClientId(request.getClientId());
        feedback.setRating(rating);
        feedback.setComment(comment);
        feedback.setTagIds(tagIds);

        ClientFeedback saved = clientFeedbackPersistencePort.save(feedback);
        replaceClientTags(saved.getId(), tagIds);

        publishRatingEvents(FeedbackSide.CLIENT, request, previousRating, rating);
        publishReviewEvents(FeedbackSide.CLIENT, request, hadComment, comment != null, previousTags, tagItems);
    }

    private void removeServiceFeedback(Long requestId) {
        ServiceFeedback feedback = serviceFeedbackPersistencePort.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service feedback not found for requestId: " + requestId));
        ServiceRequest request = loadRequest(requestId);
        List<FeedbackTagEventItem> previousTags = resolveExistingServiceTagItems(feedback.getId());

        serviceFeedbackTagPersistencePort.deleteByFeedbackId(feedback.getId());
        serviceFeedbackPersistencePort.deleteById(feedback.getId());

        if (feedback.hasRating()) {
            eventPublisher.publishEvent(new ServiceRatedEvent(
                    request.getServiceId(), request.getOffererId(), null, feedback.getRating()));
        }
        if (feedback.hasComment() || !previousTags.isEmpty()) {
            eventPublisher.publishEvent(new ReviewDeletedEvent(
                    FeedbackSide.SERVICE,
                    request.getServiceId(),
                    request.getOffererId(),
                    request.getClientId(),
                    previousTags));
        }
    }

    private void removeClientFeedback(Long requestId) {
        ClientFeedback feedback = clientFeedbackPersistencePort.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client feedback not found for requestId: " + requestId));
        ServiceRequest request = loadRequest(requestId);
        List<FeedbackTagEventItem> previousTags = resolveExistingClientTagItems(feedback.getId());

        clientFeedbackTagPersistencePort.deleteByFeedbackId(feedback.getId());
        clientFeedbackPersistencePort.deleteById(feedback.getId());

        if (feedback.hasRating()) {
            eventPublisher.publishEvent(new ClientRatedEvent(
                    request.getClientId(), request.getOffererId(), null, feedback.getRating()));
        }
        if (feedback.hasComment() || !previousTags.isEmpty()) {
            eventPublisher.publishEvent(new ReviewDeletedEvent(
                    FeedbackSide.CLIENT,
                    request.getServiceId(),
                    request.getOffererId(),
                    request.getClientId(),
                    previousTags));
        }
    }

    private void publishRatingEvents(FeedbackSide side, ServiceRequest request,
                                     Integer previousRating, Integer newRating) {
        if (Objects.equals(previousRating, newRating)) {
            return;
        }
        if (side == FeedbackSide.SERVICE) {
            eventPublisher.publishEvent(new ServiceRatedEvent(
                    request.getServiceId(), request.getOffererId(), newRating, previousRating));
        } else {
            eventPublisher.publishEvent(new ClientRatedEvent(
                    request.getClientId(), request.getOffererId(), newRating, previousRating));
        }
    }

    private void publishReviewEvents(FeedbackSide side, ServiceRequest request,
                                     boolean hadComment, boolean hasComment,
                                     List<FeedbackTagEventItem> previousTags,
                                     List<FeedbackTagEventItem> newTags) {
        boolean reviewRemoved = hadComment && !hasComment;
        boolean reviewAdded = !hadComment && hasComment;
        boolean tagsChanged = !Objects.equals(previousTags, newTags);

        if (reviewRemoved || (tagsChanged && hasComment)) {
            eventPublisher.publishEvent(new ReviewDeletedEvent(
                    side, request.getServiceId(), request.getOffererId(), request.getClientId(), previousTags));
        }
        if (reviewAdded || (tagsChanged && hasComment)) {
            eventPublisher.publishEvent(new ReviewCreatedEvent(
                    side, request.getServiceId(), request.getOffererId(), request.getClientId(), newTags));
        } else if (hadComment && hasComment && tagsChanged) {
            eventPublisher.publishEvent(new ReviewCreatedEvent(
                    side, request.getServiceId(), request.getOffererId(), request.getClientId(), newTags));
        }
    }

    private List<FeedbackTagEventItem> resolveExistingServiceTagItems(Long feedbackId) {
        return serviceFeedbackTagPersistencePort.findTagIdsByFeedbackId(feedbackId).stream()
                .map(tagId -> serviceFeedbackTagCatalogPersistencePort.findById(tagId)
                        .map(tag -> new FeedbackTagEventItem(tag.getId(), tag.getSentiment()))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<FeedbackTagEventItem> resolveExistingClientTagItems(Long feedbackId) {
        return clientFeedbackTagPersistencePort.findTagIdsByFeedbackId(feedbackId).stream()
                .map(tagId -> clientFeedbackTagCatalogPersistencePort.findById(tagId)
                        .map(tag -> new FeedbackTagEventItem(tag.getId(), tag.getSentiment()))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<FeedbackTagEventItem> resolveTagItems(FeedbackSide side, List<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return List.of();
        }
        List<FeedbackTagEventItem> items = new ArrayList<>();
        for (Long tagId : tagIds) {
            if (side == FeedbackSide.SERVICE) {
                ServiceFeedbackTagCatalog tag = serviceFeedbackTagCatalogPersistencePort.findById(tagId)
                        .orElseThrow(() -> new ResourceNotFoundException("Service feedback tag not found: " + tagId));
                items.add(new FeedbackTagEventItem(tag.getId(), tag.getSentiment()));
            } else {
                ClientFeedbackTagCatalog tag = clientFeedbackTagCatalogPersistencePort.findById(tagId)
                        .orElseThrow(() -> new ResourceNotFoundException("Client feedback tag not found: " + tagId));
                items.add(new FeedbackTagEventItem(tag.getId(), tag.getSentiment()));
            }
        }
        return items;
    }

    private void replaceServiceTags(Long feedbackId, List<Long> tagIds) {
        serviceFeedbackTagPersistencePort.deleteByFeedbackId(feedbackId);
        serviceFeedbackTagPersistencePort.addTags(feedbackId, tagIds);
    }

    private void replaceClientTags(Long feedbackId, List<Long> tagIds) {
        clientFeedbackTagPersistencePort.deleteByFeedbackId(feedbackId);
        clientFeedbackTagPersistencePort.addTags(feedbackId, tagIds);
    }

    private ServiceRequest loadRequest(Long requestId) {
        return serviceRequestPersistencePort.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + requestId));
    }

    private void requireFeedbackEligible(ServiceRequest request) {
        if (!request.isPresumablyCompleted() && !request.isCompleted()) {
            throw new InvalidStateException(
                    "Solo se puede calificar una solicitud presuntamente cumplida o completada");
        }
    }

    private void validateRating(Integer rating) {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new InvalidStateException("La calificación debe estar entre 1 y 5");
        }
    }

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.trim();
    }
}
