package com.parosurvivors.serviya.feedback.application.services;

import com.parosurvivors.serviya.feedback.application.dto.FeedbackParts;
import com.parosurvivors.serviya.feedback.application.dto.command.SubmitClientFeedbackCommand;
import com.parosurvivors.serviya.feedback.application.dto.result.ClientFeedbackResult;
import com.parosurvivors.serviya.feedback.application.ports.input.ClientFeedbackServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.FeedbackFlowPort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackTagCatalogPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackTagPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ClientFeedback;
import com.parosurvivors.serviya.feedback.domain.ClientFeedbackTagCatalog;
import com.parosurvivors.serviya.requests.application.ports.output.ServiceRequestPersistencePort;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import com.parosurvivors.serviya.shared.exceptions.UnauthorizedException;
import com.parosurvivors.serviya.users.application.ports.input.UserRoleServicePort;
import com.parosurvivors.serviya.users.domain.RoleName;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ClientFeedbackService implements ClientFeedbackServicePort {

    private final FeedbackFlowPort feedbackFlowPort;
    private final ClientFeedbackPersistencePort clientFeedbackPersistencePort;
    private final ClientFeedbackTagPersistencePort clientFeedbackTagPersistencePort;
    private final ClientFeedbackTagCatalogPersistencePort clientFeedbackTagCatalogPersistencePort;
    private final ServiceRequestPersistencePort serviceRequestPersistencePort;
    private final UserRoleServicePort userRoleServicePort;

    @Override
    @Transactional
    public void submitClientFeedback(SubmitClientFeedbackCommand command) {
        ServiceRequest request = loadRequest(command.requestId());
        if (!request.getOffererId().equals(command.offererId())) {
            throw new UnauthorizedException("Solo el oferente de la solicitud puede calificar al cliente");
        }
        if (!request.getClientId().equals(command.clientId())) {
            throw new UnauthorizedException("El cliente calificado no coincide con la solicitud");
        }
        feedbackFlowPort.submit(
                FeedbackParts.client(),
                command.requestId(),
                command.rating(),
                command.comment(),
                command.tagIds());
    }

    @Override
    public ClientFeedbackResult getClientFeedback(Long requestId) {
        ClientFeedback feedback = clientFeedbackPersistencePort.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client feedback not found for requestId: " + requestId));
        hydrateTags(feedback);
        return toResult(feedback);
    }

    @Override
    public Page<ClientFeedbackResult> getClientFeedbackList(Long clientId, Pageable pageable) {
        return clientFeedbackPersistencePort.findByClientId(clientId, pageable)
                .map(this::hydrateAndMap);
    }

    @Override
    public Page<ClientFeedbackResult> getClientFeedbackByOfferer(Long offererId, Pageable pageable) {
        return clientFeedbackPersistencePort.findByOffererId(offererId, pageable)
                .map(this::hydrateAndMap);
    }

    @Override
    @Transactional
    public boolean revertFeedback(Long requestId) {
        if (clientFeedbackPersistencePort.findByRequestId(requestId).isEmpty()) {
            return false;
        }
        feedbackFlowPort.remove(FeedbackParts.client(), requestId);
        return true;
    }

    /**
     * RF-047: oferente con solicitud común o ADMIN pueden ver feedback recibido por un cliente.
     */
    @Override
    public void requireClientFeedbackListAccess(Long viewerId, Long clientId) {
        if (userRoleServicePort.hasRole(viewerId, RoleName.ADMIN.name())) {
            return;
        }
        boolean sharedRequest = serviceRequestPersistencePort.findByClientId(clientId).stream()
                .anyMatch(request -> viewerId.equals(request.getOffererId()));
        if (!sharedRequest) {
            throw new UnauthorizedException("No autorizado para ver el feedback de este cliente");
        }
    }

    @Override
    public void requireOffererFeedbackAccess(Long viewerId, Long offererId) {
        if (viewerId.equals(offererId)) {
            return;
        }
        if (userRoleServicePort.hasRole(viewerId, RoleName.ADMIN.name())) {
            return;
        }
        throw new UnauthorizedException("No autorizado para ver el historial de feedback del oferente");
    }

    @Override
    public void requireRequestPartyAccess(Long viewerId, Long requestId) {
        ServiceRequest request = loadRequest(requestId);
        if (viewerId.equals(request.getClientId()) || viewerId.equals(request.getOffererId())) {
            return;
        }
        if (userRoleServicePort.hasRole(viewerId, RoleName.ADMIN.name())) {
            return;
        }
        throw new UnauthorizedException("No autorizado para ver el feedback de esta solicitud");
    }

    private ClientFeedbackResult hydrateAndMap(ClientFeedback feedback) {
        hydrateTags(feedback);
        return toResult(feedback);
    }

    private void hydrateTags(ClientFeedback feedback) {
        if (feedback.getId() == null) {
            return;
        }
        feedback.setTagIds(clientFeedbackTagPersistencePort.findTagIdsByFeedbackId(feedback.getId()));
    }

    private ClientFeedbackResult toResult(ClientFeedback feedback) {
        List<String> tagNames = feedback.getTagIds().stream()
                .map(tagId -> clientFeedbackTagCatalogPersistencePort.findById(tagId)
                        .map(ClientFeedbackTagCatalog::getTagName)
                        .orElse(null))
                .filter(name -> name != null)
                .toList();
        return new ClientFeedbackResult(
                feedback.getRequestId(),
                feedback.getClientId(),
                feedback.getOffererId(),
                feedback.getRating(),
                feedback.getComment(),
                tagNames,
                feedback.getCreatedAt());
    }

    private ServiceRequest loadRequest(Long requestId) {
        return serviceRequestPersistencePort.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + requestId));
    }
}
