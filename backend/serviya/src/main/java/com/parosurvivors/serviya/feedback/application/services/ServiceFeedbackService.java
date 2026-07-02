package com.parosurvivors.serviya.feedback.application.services;

import com.parosurvivors.serviya.feedback.application.dto.FeedbackParts;
import com.parosurvivors.serviya.feedback.application.dto.command.SubmitServiceFeedbackCommand;
import com.parosurvivors.serviya.feedback.application.dto.result.ServiceFeedbackResult;
import com.parosurvivors.serviya.feedback.application.ports.input.FeedbackFlowPort;
import com.parosurvivors.serviya.feedback.application.ports.input.ServiceFeedbackServicePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackTagCatalogPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackTagPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ServiceFeedback;
import com.parosurvivors.serviya.feedback.domain.ServiceFeedbackTagCatalog;
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
public class ServiceFeedbackService implements ServiceFeedbackServicePort {

    private final FeedbackFlowPort feedbackFlowPort;
    private final ServiceFeedbackPersistencePort serviceFeedbackPersistencePort;
    private final ServiceFeedbackTagPersistencePort serviceFeedbackTagPersistencePort;
    private final ServiceFeedbackTagCatalogPersistencePort serviceFeedbackTagCatalogPersistencePort;
    private final ServiceRequestPersistencePort serviceRequestPersistencePort;
    private final UserRoleServicePort userRoleServicePort;

    @Override
    @Transactional
    public void submitServiceFeedback(SubmitServiceFeedbackCommand command) {
        ServiceRequest request = loadRequest(command.requestId());
        if (!request.getClientId().equals(command.clientId())) {
            throw new UnauthorizedException("Solo el cliente de la solicitud puede calificar el servicio");
        }
        feedbackFlowPort.submit(
                FeedbackParts.service(),
                command.requestId(),
                command.rating(),
                command.comment(),
                command.tagIds());
    }

    @Override
    public ServiceFeedbackResult getServiceFeedback(Long requestId) {
        ServiceFeedback feedback = serviceFeedbackPersistencePort.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service feedback not found for requestId: " + requestId));
        hydrateTags(feedback);
        return toResult(feedback);
    }

    @Override
    public Page<ServiceFeedbackResult> getServiceFeedbackList(Long serviceId, Pageable pageable) {
        return serviceFeedbackPersistencePort.findByServiceId(serviceId, pageable)
                .map(this::hydrateAndMap);
    }

    @Override
    public Page<ServiceFeedbackResult> getServiceFeedbackByClient(Long clientId, Pageable pageable) {
        return serviceFeedbackPersistencePort.findByClientId(clientId, pageable)
                .map(this::hydrateAndMap);
    }

    @Override
    public List<ServiceFeedback> getRecentServiceFeedback(Long serviceId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<ServiceFeedback> feedbackList = serviceFeedbackPersistencePort.findRecentByServiceId(serviceId, limit);
        feedbackList.forEach(this::hydrateTags);
        return feedbackList;
    }

    @Override
    @Transactional
    public boolean revertFeedback(Long requestId) {
        if (serviceFeedbackPersistencePort.findByRequestId(requestId).isEmpty()) {
            return false;
        }
        feedbackFlowPort.remove(FeedbackParts.service(), requestId);
        return true;
    }

    @Override
    public void requireClientFeedbackAccess(Long viewerId, Long clientId) {
        if (viewerId.equals(clientId)) {
            return;
        }
        if (userRoleServicePort.hasRole(viewerId, RoleName.ADMIN.name())) {
            return;
        }
        throw new UnauthorizedException("No autorizado para ver el feedback de este cliente");
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

    private ServiceFeedbackResult hydrateAndMap(ServiceFeedback feedback) {
        hydrateTags(feedback);
        return toResult(feedback);
    }

    private void hydrateTags(ServiceFeedback feedback) {
        if (feedback.getId() == null) {
            return;
        }
        feedback.setTagIds(serviceFeedbackTagPersistencePort.findTagIdsByFeedbackId(feedback.getId()));
    }

    private ServiceFeedbackResult toResult(ServiceFeedback feedback) {
        List<String> tagNames = feedback.getTagIds().stream()
                .map(tagId -> serviceFeedbackTagCatalogPersistencePort.findById(tagId)
                        .map(ServiceFeedbackTagCatalog::getTagName)
                        .orElse(null))
                .filter(name -> name != null)
                .toList();
        return new ServiceFeedbackResult(
                feedback.getRequestId(),
                feedback.getServiceId(),
                feedback.getClientId(),
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
