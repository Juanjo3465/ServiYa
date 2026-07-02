package com.parosurvivors.serviya.feedback.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parosurvivors.serviya.feedback.application.dto.FeedbackParts;
import com.parosurvivors.serviya.feedback.application.dto.FeedbackSide;
import com.parosurvivors.serviya.feedback.application.events.ReviewCreatedEvent;
import com.parosurvivors.serviya.feedback.application.events.ReviewDeletedEvent;
import com.parosurvivors.serviya.feedback.application.events.ServiceRatedEvent;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackTagCatalogPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackTagPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackTagCatalogPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackTagPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ServiceFeedback;
import com.parosurvivors.serviya.feedback.domain.ServiceFeedbackTagCatalog;
import com.parosurvivors.serviya.feedback.domain.TagSentiment;
import com.parosurvivors.serviya.requests.application.ports.output.ServiceRequestPersistencePort;
import com.parosurvivors.serviya.requests.domain.RequestStatus;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import com.parosurvivors.serviya.shared.exceptions.InvalidStateException;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class FeedbackFlowTest {

    @Mock ServiceRequestPersistencePort serviceRequestPersistencePort;
    @Mock ServiceFeedbackPersistencePort serviceFeedbackPersistencePort;
    @Mock ServiceFeedbackTagPersistencePort serviceFeedbackTagPersistencePort;
    @Mock ServiceFeedbackTagCatalogPersistencePort serviceFeedbackTagCatalogPersistencePort;
    @Mock ClientFeedbackPersistencePort clientFeedbackPersistencePort;
    @Mock ClientFeedbackTagPersistencePort clientFeedbackTagPersistencePort;
    @Mock ClientFeedbackTagCatalogPersistencePort clientFeedbackTagCatalogPersistencePort;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks FeedbackFlow feedbackFlow;

    @Test
    void submitServiceFeedback_persistsNormalizedFeedbackTagsAndEvents() {
        ServiceRequest request = completedRequest();
        ServiceFeedback saved = ServiceFeedback.builder().id(99L).build();
        when(serviceRequestPersistencePort.findById(10L)).thenReturn(Optional.of(request));
        when(serviceFeedbackPersistencePort.findByRequestId(10L)).thenReturn(Optional.empty());
        when(serviceFeedbackTagCatalogPersistencePort.findById(7L))
                .thenReturn(Optional.of(tag(7L, "Puntual", TagSentiment.P)));
        when(serviceFeedbackPersistencePort.save(any(ServiceFeedback.class))).thenReturn(saved);

        feedbackFlow.submit(FeedbackParts.service(), 10L, 5, "  Excelente servicio  ", List.of(7L));

        ArgumentCaptor<ServiceFeedback> feedbackCaptor = ArgumentCaptor.forClass(ServiceFeedback.class);
        verify(serviceFeedbackPersistencePort).save(feedbackCaptor.capture());
        ServiceFeedback feedback = feedbackCaptor.getValue();
        assertThat(feedback.getRequestId()).isEqualTo(10L);
        assertThat(feedback.getClientId()).isEqualTo(20L);
        assertThat(feedback.getServiceId()).isEqualTo(30L);
        assertThat(feedback.getRating()).isEqualTo(5);
        assertThat(feedback.getComment()).isEqualTo("Excelente servicio");
        assertThat(feedback.getTagIds()).containsExactly(7L);

        verify(serviceFeedbackTagPersistencePort).deleteByFeedbackId(99L);
        verify(serviceFeedbackTagPersistencePort).addTags(99L, List.of(7L));
        verify(eventPublisher).publishEvent(any(ServiceRatedEvent.class));
        verify(eventPublisher).publishEvent(any(ReviewCreatedEvent.class));
    }

    @Test
    void submit_doesNothing_whenFeedbackIsEmptyAfterNormalization() {
        when(serviceRequestPersistencePort.findById(10L)).thenReturn(Optional.of(completedRequest()));

        feedbackFlow.submit(FeedbackParts.service(), 10L, null, "   ", null);

        verify(serviceFeedbackPersistencePort, never()).save(any(ServiceFeedback.class));
        verify(serviceFeedbackTagPersistencePort, never()).addTags(anyLong(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void submit_rejectsRequestsThatAreNotCompletedEnough() {
        ServiceRequest request = completedRequest();
        request.setStatus(RequestStatus.PENDING);
        request.setCompletedAt(null);
        when(serviceRequestPersistencePort.findById(10L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> feedbackFlow.submit(FeedbackParts.service(), 10L, 5, null, List.of()))
                .isInstanceOf(InvalidStateException.class);

        verify(serviceFeedbackPersistencePort, never()).save(any(ServiceFeedback.class));
    }

    @Test
    void submit_rejectsRatingOutsideValidRange() {
        when(serviceRequestPersistencePort.findById(10L)).thenReturn(Optional.of(completedRequest()));

        assertThatThrownBy(() -> feedbackFlow.submit(FeedbackParts.service(), 10L, 6, null, List.of()))
                .isInstanceOf(InvalidStateException.class);

        verify(serviceFeedbackPersistencePort, never()).save(any(ServiceFeedback.class));
    }

    @Test
    void submit_rejectsUnknownTag() {
        when(serviceRequestPersistencePort.findById(10L)).thenReturn(Optional.of(completedRequest()));
        when(serviceFeedbackTagCatalogPersistencePort.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackFlow.submit(FeedbackParts.service(), 10L, 4, "Bien", List.of(404L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeServiceFeedback_deletesFeedbackTagsAndPublishesRemovalEvents() {
        ServiceFeedback existing = ServiceFeedback.builder()
                .id(99L)
                .requestId(10L)
                .rating(4)
                .comment("Buen trabajo")
                .build();
        when(serviceFeedbackPersistencePort.findByRequestId(10L)).thenReturn(Optional.of(existing));
        when(serviceRequestPersistencePort.findById(10L)).thenReturn(Optional.of(completedRequest()));
        when(serviceFeedbackTagPersistencePort.findTagIdsByFeedbackId(99L)).thenReturn(List.of(7L));
        when(serviceFeedbackTagCatalogPersistencePort.findById(7L))
                .thenReturn(Optional.of(tag(7L, "Puntual", TagSentiment.P)));

        feedbackFlow.remove(FeedbackParts.service(), 10L);

        verify(serviceFeedbackTagPersistencePort).deleteByFeedbackId(99L);
        verify(serviceFeedbackPersistencePort).deleteById(99L);
        verify(eventPublisher).publishEvent(any(ServiceRatedEvent.class));

        ArgumentCaptor<ReviewDeletedEvent> eventCaptor = ArgumentCaptor.forClass(ReviewDeletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        ReviewDeletedEvent event = eventCaptor.getValue();
        assertThat(event.side()).isEqualTo(FeedbackSide.SERVICE);
        assertThat(event.serviceId()).isEqualTo(30L);
        assertThat(event.tags()).extracting("tagId").containsExactly(7L);
    }

    private ServiceRequest completedRequest() {
        return ServiceRequest.builder()
                .id(10L)
                .clientId(20L)
                .serviceId(30L)
                .offererId(40L)
                .status(RequestStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();
    }

    private ServiceFeedbackTagCatalog tag(Long id, String name, TagSentiment sentiment) {
        return ServiceFeedbackTagCatalog.builder()
                .id(id)
                .tagName(name)
                .sentiment(sentiment)
                .build();
    }
}
