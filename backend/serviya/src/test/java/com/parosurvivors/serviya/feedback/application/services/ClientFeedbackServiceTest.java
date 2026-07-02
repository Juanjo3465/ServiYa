package com.parosurvivors.serviya.feedback.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parosurvivors.serviya.feedback.application.dto.FeedbackParts;
import com.parosurvivors.serviya.feedback.application.dto.command.SubmitClientFeedbackCommand;
import com.parosurvivors.serviya.feedback.application.dto.result.ClientFeedbackResult;
import com.parosurvivors.serviya.feedback.application.ports.input.FeedbackFlowPort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackTagCatalogPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackTagPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ClientFeedback;
import com.parosurvivors.serviya.feedback.domain.ClientFeedbackTagCatalog;
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
class ClientFeedbackServiceTest {

    @Mock FeedbackFlowPort feedbackFlowPort;
    @Mock ClientFeedbackPersistencePort clientFeedbackPersistencePort;
    @Mock ClientFeedbackTagPersistencePort clientFeedbackTagPersistencePort;
    @Mock ClientFeedbackTagCatalogPersistencePort clientFeedbackTagCatalogPersistencePort;
    @Mock ServiceRequestPersistencePort serviceRequestPersistencePort;
    @Mock UserRoleServicePort userRoleServicePort;

    @InjectMocks ClientFeedbackService service;

    @Test
    void submitClientFeedback_delegatesToSharedFlow_whenRequesterIsOffererAndClientMatches() {
        when(serviceRequestPersistencePort.findById(10L)).thenReturn(Optional.of(request()));

        service.submitClientFeedback(new SubmitClientFeedbackCommand(
                40L, 10L, 20L, 5, "Cliente cumplido", List.of(7L)));

        verify(feedbackFlowPort).submit(
                eq(FeedbackParts.client()),
                eq(10L),
                eq(5),
                eq("Cliente cumplido"),
                eq(List.of(7L)));
    }

    @Test
    void submitClientFeedback_rejectsUsersThatAreNotTheRequestOfferer() {
        when(serviceRequestPersistencePort.findById(10L)).thenReturn(Optional.of(request()));

        assertThatThrownBy(() -> service.submitClientFeedback(new SubmitClientFeedbackCommand(
                999L, 10L, 20L, 5, "Cliente cumplido", List.of(7L))))
                .isInstanceOf(UnauthorizedException.class);

        verify(feedbackFlowPort, never()).submit(any(), any(), any(), any(), any());
    }

    @Test
    void submitClientFeedback_rejectsClientThatDoesNotMatchRequest() {
        when(serviceRequestPersistencePort.findById(10L)).thenReturn(Optional.of(request()));

        assertThatThrownBy(() -> service.submitClientFeedback(new SubmitClientFeedbackCommand(
                40L, 10L, 999L, 5, "Cliente cumplido", List.of(7L))))
                .isInstanceOf(UnauthorizedException.class);

        verify(feedbackFlowPort, never()).submit(any(), any(), any(), any(), any());
    }

    @Test
    void getClientFeedback_hydratesTagNames() {
        ClientFeedback feedback = ClientFeedback.builder()
                .id(99L)
                .requestId(10L)
                .clientId(20L)
                .offererId(40L)
                .rating(4)
                .comment("Responsable")
                .createdAt(LocalDateTime.of(2026, 1, 15, 10, 30))
                .build();
        when(clientFeedbackPersistencePort.findByRequestId(10L)).thenReturn(Optional.of(feedback));
        when(clientFeedbackTagPersistencePort.findTagIdsByFeedbackId(99L)).thenReturn(List.of(7L, 8L));
        when(clientFeedbackTagCatalogPersistencePort.findById(7L))
                .thenReturn(Optional.of(tag(7L, "Puntual")));
        when(clientFeedbackTagCatalogPersistencePort.findById(8L))
                .thenReturn(Optional.of(tag(8L, "Comunicativo")));

        ClientFeedbackResult result = service.getClientFeedback(10L);

        assertThat(result.requestId()).isEqualTo(10L);
        assertThat(result.clientId()).isEqualTo(20L);
        assertThat(result.offererId()).isEqualTo(40L);
        assertThat(result.rating()).isEqualTo(4);
        assertThat(result.comment()).isEqualTo("Responsable");
        assertThat(result.tags()).containsExactly("Puntual", "Comunicativo");
    }

    @Test
    void requireClientFeedbackListAccess_allowsOffererWithSharedRequest() {
        when(userRoleServicePort.hasRole(40L, "ADMIN")).thenReturn(false);
        when(serviceRequestPersistencePort.findByClientId(20L)).thenReturn(List.of(request()));

        service.requireClientFeedbackListAccess(40L, 20L);

        verify(serviceRequestPersistencePort).findByClientId(20L);
    }

    @Test
    void requireClientFeedbackListAccess_rejectsViewerWithoutSharedRequest() {
        when(userRoleServicePort.hasRole(999L, "ADMIN")).thenReturn(false);
        when(serviceRequestPersistencePort.findByClientId(20L)).thenReturn(List.of(request()));

        assertThatThrownBy(() -> service.requireClientFeedbackListAccess(999L, 20L))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void revertFeedback_removesThroughSharedFlow_whenFeedbackExists() {
        when(clientFeedbackPersistencePort.findByRequestId(10L))
                .thenReturn(Optional.of(ClientFeedback.builder().id(99L).build()));

        boolean result = service.revertFeedback(10L);

        assertThat(result).isTrue();
        verify(feedbackFlowPort).remove(FeedbackParts.client(), 10L);
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

    private ClientFeedbackTagCatalog tag(Long id, String name) {
        return ClientFeedbackTagCatalog.builder()
                .id(id)
                .tagName(name)
                .sentiment(TagSentiment.P)
                .build();
    }
}
