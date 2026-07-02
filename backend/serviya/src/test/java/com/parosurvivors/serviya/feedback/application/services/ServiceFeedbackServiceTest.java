package com.parosurvivors.serviya.feedback.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parosurvivors.serviya.feedback.application.dto.FeedbackParts;
import com.parosurvivors.serviya.feedback.application.dto.FeedbackSide;
import com.parosurvivors.serviya.feedback.application.dto.command.SubmitServiceFeedbackCommand;
import com.parosurvivors.serviya.feedback.application.dto.result.ServiceFeedbackResult;
import com.parosurvivors.serviya.feedback.application.ports.input.FeedbackFlowPort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackTagCatalogPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackTagPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ServiceFeedback;
import com.parosurvivors.serviya.feedback.domain.ServiceFeedbackTagCatalog;
import com.parosurvivors.serviya.feedback.domain.TagSentiment;
import com.parosurvivors.serviya.requests.application.ports.output.ServiceRequestPersistencePort;
import com.parosurvivors.serviya.requests.domain.RequestStatus;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import com.parosurvivors.serviya.shared.exceptions.UnauthorizedException;
import com.parosurvivors.serviya.users.application.ports.input.UserRoleServicePort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceFeedbackServiceTest {

    @Mock FeedbackFlowPort feedbackFlowPort;
    @Mock ServiceFeedbackPersistencePort serviceFeedbackPersistencePort;
    @Mock ServiceFeedbackTagPersistencePort serviceFeedbackTagPersistencePort;
    @Mock ServiceFeedbackTagCatalogPersistencePort serviceFeedbackTagCatalogPersistencePort;
    @Mock ServiceRequestPersistencePort serviceRequestPersistencePort;
    @Mock UserRoleServicePort userRoleServicePort;

    @InjectMocks ServiceFeedbackService service;

    @Test
    void submitServiceFeedback_delegatesToSharedFlow_whenRequesterIsClient() {
        when(serviceRequestPersistencePort.findById(10L)).thenReturn(Optional.of(request()));

        service.submitServiceFeedback(new SubmitServiceFeedbackCommand(
                20L, 10L, 5, "Muy bien", List.of(7L)));

        verify(feedbackFlowPort).submit(
                eq(FeedbackParts.service()),
                eq(10L),
                eq(5),
                eq("Muy bien"),
                eq(List.of(7L)));
    }

    @Test
    void submitServiceFeedback_rejectsUsersThatAreNotTheRequestClient() {
        when(serviceRequestPersistencePort.findById(10L)).thenReturn(Optional.of(request()));

        assertThatThrownBy(() -> service.submitServiceFeedback(new SubmitServiceFeedbackCommand(
                999L, 10L, 5, "Muy bien", List.of(7L))))
                .isInstanceOf(UnauthorizedException.class);

        verify(feedbackFlowPort, never()).submit(any(), any(), any(), any(), any());
    }

    @Test
    void getServiceFeedback_hydratesTagNames() {
        ServiceFeedback feedback = ServiceFeedback.builder()
                .id(99L)
                .requestId(10L)
                .serviceId(30L)
                .clientId(20L)
                .rating(4)
                .comment("Correcto")
                .createdAt(LocalDateTime.of(2026, 1, 15, 10, 30))
                .build();
        when(serviceFeedbackPersistencePort.findByRequestId(10L)).thenReturn(Optional.of(feedback));
        when(serviceFeedbackTagPersistencePort.findTagIdsByFeedbackId(99L)).thenReturn(List.of(7L, 8L));
        when(serviceFeedbackTagCatalogPersistencePort.findById(7L))
                .thenReturn(Optional.of(tag(7L, "Puntual")));
        when(serviceFeedbackTagCatalogPersistencePort.findById(8L))
                .thenReturn(Optional.of(tag(8L, "Amable")));

        ServiceFeedbackResult result = service.getServiceFeedback(10L);

        assertThat(result.requestId()).isEqualTo(10L);
        assertThat(result.serviceId()).isEqualTo(30L);
        assertThat(result.clientId()).isEqualTo(20L);
        assertThat(result.rating()).isEqualTo(4);
        assertThat(result.comment()).isEqualTo("Correcto");
        assertThat(result.tags()).containsExactly("Puntual", "Amable");
    }

    @Test
    void revertFeedback_returnsFalseWithoutCallingFlow_whenFeedbackDoesNotExist() {
        when(serviceFeedbackPersistencePort.findByRequestId(10L)).thenReturn(Optional.empty());

        boolean result = service.revertFeedback(10L);

        assertThat(result).isFalse();
        verify(feedbackFlowPort, never()).remove(any(), any());
    }

    @Test
    void requireRequestPartyAccess_allowsAdminViewer() {
        when(serviceRequestPersistencePort.findById(10L)).thenReturn(Optional.of(request()));
        when(userRoleServicePort.hasRole(999L, "ADMIN")).thenReturn(true);

        service.requireRequestPartyAccess(999L, 10L);

        verify(userRoleServicePort).hasRole(999L, "ADMIN");
    }

    @Test
    void requireRequestPartyAccess_rejectsUnrelatedViewer() {
        when(serviceRequestPersistencePort.findById(10L)).thenReturn(Optional.of(request()));
        when(userRoleServicePort.hasRole(999L, "ADMIN")).thenReturn(false);

        assertThatThrownBy(() -> service.requireRequestPartyAccess(999L, 10L))
                .isInstanceOf(UnauthorizedException.class);
    }

    private ServiceRequest request() {
        return ServiceRequest.builder()
                .id(10L)
                .clientId(20L)
                .serviceId(30L)
                .offererId(40L)
                .status(RequestStatus.COMPLETED)
                .build();
    }

    private ServiceFeedbackTagCatalog tag(Long id, String name) {
        return ServiceFeedbackTagCatalog.builder()
                .id(id)
                .tagName(name)
                .sentiment(TagSentiment.P)
                .build();
    }
}
