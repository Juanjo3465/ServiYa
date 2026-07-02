package com.parosurvivors.serviya.metrics.application.services;

import com.parosurvivors.serviya.feedback.application.dto.FeedbackSide;
import com.parosurvivors.serviya.feedback.application.events.ClientRatedEvent;
import com.parosurvivors.serviya.feedback.application.events.FeedbackTagEventItem;
import com.parosurvivors.serviya.feedback.application.events.ReviewCreatedEvent;
import com.parosurvivors.serviya.feedback.application.events.ReviewDeletedEvent;
import com.parosurvivors.serviya.feedback.application.events.ServiceRatedEvent;
import com.parosurvivors.serviya.metrics.application.ports.output.ClientMetricsPersistencePort;
import com.parosurvivors.serviya.metrics.application.ports.output.ClientTagMetricsPersistencePort;
import com.parosurvivors.serviya.metrics.application.ports.output.OffererMetricsPersistencePort;
import com.parosurvivors.serviya.metrics.application.ports.output.OffererTagMetricsPersistencePort;
import com.parosurvivors.serviya.metrics.application.ports.output.ServiceMetricsPersistencePort;
import com.parosurvivors.serviya.metrics.application.ports.output.ServiceTagMetricsPersistencePort;
import com.parosurvivors.serviya.metrics.domain.ClientMetrics;
import com.parosurvivors.serviya.metrics.domain.ClientTagMetrics;
import com.parosurvivors.serviya.metrics.domain.OffererMetrics;
import com.parosurvivors.serviya.metrics.domain.OffererTagMetrics;
import com.parosurvivors.serviya.metrics.domain.ServiceMetrics;
import com.parosurvivors.serviya.metrics.domain.ServiceTagMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Actualiza métricas en respuesta a eventos del módulo feedback (@EventListener).
 * Patrón UPSERT documentado en diagrama-clases.md (módulo 6).
 */
@Component
@RequiredArgsConstructor
public class FeedbackMetricsEventHandler {

    private final ServiceMetricsPersistencePort serviceMetricsPersistencePort;
    private final ServiceTagMetricsPersistencePort serviceTagMetricsPersistencePort;
    private final OffererMetricsPersistencePort offererMetricsPersistencePort;
    private final OffererTagMetricsPersistencePort offererTagMetricsPersistencePort;
    private final ClientMetricsPersistencePort clientMetricsPersistencePort;
    private final ClientTagMetricsPersistencePort clientTagMetricsPersistencePort;

    @EventListener
    @Transactional
    public void onServiceRated(ServiceRatedEvent event) {
        ServiceMetrics serviceMetrics = loadOrCreateServiceMetrics(event.serviceId());
        applyRatingChange(serviceMetrics, event.previousRating(), event.newRating());
        serviceMetricsPersistencePort.save(serviceMetrics);

        OffererMetrics offererMetrics = loadOrCreateOffererMetrics(event.offererId());
        applyRatingChange(offererMetrics, event.previousRating(), event.newRating());
        offererMetricsPersistencePort.save(offererMetrics);
    }

    @EventListener
    @Transactional
    public void onClientRated(ClientRatedEvent event) {
        ClientMetrics clientMetrics = loadOrCreateClientMetrics(event.clientId());
        applyRatingChange(clientMetrics, event.previousRating(), event.newRating());
        clientMetricsPersistencePort.save(clientMetrics);
    }

    @EventListener
    @Transactional
    public void onReviewCreated(ReviewCreatedEvent event) {
        if (event.side() == FeedbackSide.SERVICE) {
            ServiceMetrics serviceMetrics = loadOrCreateServiceMetrics(event.serviceId());
            serviceMetrics.incrementComments();
            serviceMetricsPersistencePort.save(serviceMetrics);

            OffererMetrics offererMetrics = loadOrCreateOffererMetrics(event.offererId());
            offererMetrics.incrementComments();
            applyTagCounts(offererMetrics, event.tags(), true);
            offererMetricsPersistencePort.save(offererMetrics);

            applyServiceTagCounts(event.serviceId(), event.tags(), true);
        } else {
            ClientMetrics clientMetrics = loadOrCreateClientMetrics(event.clientId());
            clientMetrics.incrementComments();
            applyTagCounts(clientMetrics, event.tags(), true);
            clientMetricsPersistencePort.save(clientMetrics);

            applyOffererTagCounts(event.offererId(), event.tags(), true);
        }
    }

    @EventListener
    @Transactional
    public void onReviewDeleted(ReviewDeletedEvent event) {
        if (event.side() == FeedbackSide.SERVICE) {
            ServiceMetrics serviceMetrics = loadOrCreateServiceMetrics(event.serviceId());
            serviceMetrics.decrementComments();
            serviceMetricsPersistencePort.save(serviceMetrics);

            OffererMetrics offererMetrics = loadOrCreateOffererMetrics(event.offererId());
            offererMetrics.decrementComments();
            applyTagCounts(offererMetrics, event.tags(), false);
            offererMetricsPersistencePort.save(offererMetrics);

            applyServiceTagCounts(event.serviceId(), event.tags(), false);
        } else {
            ClientMetrics clientMetrics = loadOrCreateClientMetrics(event.clientId());
            clientMetrics.decrementComments();
            applyTagCounts(clientMetrics, event.tags(), false);
            clientMetricsPersistencePort.save(clientMetrics);

            applyOffererTagCounts(event.offererId(), event.tags(), false);
        }
    }

    private void applyRatingChange(ServiceMetrics metrics, Integer previous, Integer next) {
        if (Objects.equals(previous, next)) {
            return;
        }
        if (previous != null) {
            metrics.removeRating(previous);
        }
        if (next != null) {
            metrics.registerRating(next);
        }
    }

    private void applyRatingChange(OffererMetrics metrics, Integer previous, Integer next) {
        if (Objects.equals(previous, next)) {
            return;
        }
        if (previous != null) {
            metrics.removeRating(previous);
        }
        if (next != null) {
            metrics.registerRating(next);
        }
    }

    private void applyRatingChange(ClientMetrics metrics, Integer previous, Integer next) {
        if (Objects.equals(previous, next)) {
            return;
        }
        if (previous != null) {
            metrics.removeRating(previous);
        }
        if (next != null) {
            metrics.registerRating(next);
        }
    }

    private void applyTagCounts(OffererMetrics metrics, List<FeedbackTagEventItem> tags, boolean increment) {
        int positive = countBySentiment(tags, true);
        int negative = countBySentiment(tags, false);
        if (increment) {
            metrics.addPositiveTags(positive);
            metrics.addNegativeTags(negative);
        } else {
            metrics.addPositiveTags(-positive);
            metrics.addNegativeTags(-negative);
        }
    }

    private void applyTagCounts(ClientMetrics metrics, List<FeedbackTagEventItem> tags, boolean increment) {
        int positive = countBySentiment(tags, true);
        int negative = countBySentiment(tags, false);
        if (increment) {
            metrics.addPositiveTags(positive);
            metrics.addNegativeTags(negative);
        } else {
            metrics.addPositiveTags(-positive);
            metrics.addNegativeTags(-negative);
        }
    }

    private void applyServiceTagCounts(Long serviceId, List<FeedbackTagEventItem> tags, boolean increment) {
        for (FeedbackTagEventItem tag : tags) {
            ServiceTagMetrics metrics = serviceTagMetricsPersistencePort
                    .findByServiceIdAndTagId(serviceId, tag.tagId())
                    .orElse(ServiceTagMetrics.builder().serviceId(serviceId).tagId(tag.tagId()).build());
            if (increment) {
                metrics.increment();
            } else {
                metrics.decrement();
            }
            serviceTagMetricsPersistencePort.save(metrics);
        }
    }

    private void applyOffererTagCounts(Long offererId, List<FeedbackTagEventItem> tags, boolean increment) {
        for (FeedbackTagEventItem tag : tags) {
            OffererTagMetrics metrics = offererTagMetricsPersistencePort
                    .findByOffererIdAndTagId(offererId, tag.tagId())
                    .orElse(OffererTagMetrics.builder().offererId(offererId).tagId(tag.tagId()).build());
            if (increment) {
                metrics.increment();
            } else {
                metrics.decrement();
            }
            offererTagMetricsPersistencePort.save(metrics);
        }
    }

    private int countBySentiment(List<FeedbackTagEventItem> tags, boolean positive) {
        return (int) tags.stream()
                .filter(item -> item.sentiment() != null && item.sentiment().isPositive() == positive)
                .count();
    }

    private ServiceMetrics loadOrCreateServiceMetrics(Long serviceId) {
        return serviceMetricsPersistencePort.findByServiceId(serviceId)
                .orElse(ServiceMetrics.builder().serviceId(serviceId).build());
    }

    private OffererMetrics loadOrCreateOffererMetrics(Long offererId) {
        return offererMetricsPersistencePort.findByOffererId(offererId)
                .orElse(OffererMetrics.builder().offererId(offererId).build());
    }

    private ClientMetrics loadOrCreateClientMetrics(Long clientId) {
        return clientMetricsPersistencePort.findByClientId(clientId)
                .orElse(ClientMetrics.builder().clientId(clientId).build());
    }
}
