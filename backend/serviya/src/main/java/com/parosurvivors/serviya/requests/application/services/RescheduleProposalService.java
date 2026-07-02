package com.parosurvivors.serviya.requests.application.services;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.requests.application.dto.command.CreateRescheduleProposalCommand;
import com.parosurvivors.serviya.requests.application.mappers.RescheduleProposalCommandMapper;
import com.parosurvivors.serviya.requests.application.ports.input.RescheduleProposalServicePort;
import com.parosurvivors.serviya.requests.application.ports.output.RescheduleProposalPersistencePort;
import com.parosurvivors.serviya.requests.application.ports.output.ServiceRequestPersistencePort;
import com.parosurvivors.serviya.requests.domain.ProposalStatus;
import com.parosurvivors.serviya.requests.domain.RequestStatus;
import com.parosurvivors.serviya.requests.domain.RescheduleProposal;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import com.parosurvivors.serviya.shared.exceptions.InvalidStateException;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import com.parosurvivors.serviya.shared.exceptions.UnauthorizedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de propuestas de reprogramación (módulo 4). Implementa las escrituras (crear/aceptar/
 * rechazar/cancelar) y la resolución centralizada de propuestas PENDING que invocan los
 * orquestadores de {@link ServiceRequestCommandService}. Los reads siguen como placeholder.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class RescheduleProposalService implements RescheduleProposalServicePort {

    private final RescheduleProposalPersistencePort rescheduleProposalPersistencePort;
    private final ServiceRequestPersistencePort serviceRequestPersistencePort;
    private final RescheduleProposalCommandMapper commandMapper;
    private final NotificationServicePort notificationServicePort;

    @Override
    @Transactional
    public RescheduleProposal createProposal(CreateRescheduleProposalCommand command) {
        ServiceRequest request = loadRequest(command.requestId());
        requireOwnership(request.getOffererId(), command.offererId());
        if (!request.isAccepted()) {
            throw new InvalidStateException(
                    "Sólo se puede proponer una reprogramación sobre una solicitud aceptada; estado actual: "
                            + request.getStatus());
        }
        // Sólo puede haber una propuesta PENDING por solicitud: la anterior (si existe) se cancela.
        cancelPendingProposals(command.requestId());

        RescheduleProposal proposal = commandMapper.toDomain(command);
        proposal.setStatus(ProposalStatus.PENDING);
        proposal.setCreatedAt(LocalDateTime.now());
        RescheduleProposal saved = rescheduleProposalPersistencePort.save(proposal);
        // TODO(notif): notificar al cliente que el oferente propuso una nueva fecha.
        return saved;
    }

    @Override
    @Transactional
    public ServiceRequest acceptProposal(Long proposalId, Long clientId) {
        RescheduleProposal proposal = loadProposal(proposalId);
        ServiceRequest request = loadRequest(proposal.getRequestId());
        requireOwnership(request.getClientId(), clientId);

        proposal.accept();
        request.markRescheduled(clientId);
        ServiceRequest replacement = request.rescheduleTo(
                proposal.getProposedDate(), RequestStatus.ACCEPTED, clientId);

        rescheduleProposalPersistencePort.update(proposal);
        serviceRequestPersistencePort.update(request);
        ServiceRequest saved = serviceRequestPersistencePort.save(replacement);
        // TODO(notif): notificar al oferente que el cliente aceptó la propuesta.
        return saved;
    }

    @Override
    @Transactional
    public void rejectProposal(Long proposalId, Long clientId) {
        RescheduleProposal proposal = loadProposal(proposalId);
        ServiceRequest request = loadRequest(proposal.getRequestId());
        requireOwnership(request.getClientId(), clientId);

        proposal.reject();
        rescheduleProposalPersistencePort.update(proposal);
        // TODO(notif): notificar al oferente que el cliente rechazó la propuesta.
    }

    @Override
    @Transactional
    public void cancelProposal(Long proposalId, Long offererId) {
        RescheduleProposal proposal = loadProposal(proposalId);
        ServiceRequest request = loadRequest(proposal.getRequestId());
        requireOwnership(request.getOffererId(), offererId);

        proposal.cancel();
        rescheduleProposalPersistencePort.update(proposal);
        // TODO(notif): notificar al cliente que el oferente retiró la propuesta.
    }

    @Override
    @Transactional
    public int supersedePendingProposals(Long requestId) {
        return resolvePending(requestId, RescheduleProposal::markSuperseded);
    }

    @Override
    @Transactional
    public int cancelPendingProposals(Long requestId) {
        return resolvePending(requestId, RescheduleProposal::cancel);
    }

    @Override
    public List<RescheduleProposal> getProposalsForClient(Long clientId, List<String> statuses) {
        throw new UnsupportedOperationException("TODO: getProposalsForClient — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public List<RescheduleProposal> getProposalsByOfferer(Long offererId, List<String> statuses) {
        throw new UnsupportedOperationException("TODO: getProposalsByOfferer — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public List<RescheduleProposal> getProposalsByRequest(Long requestId) {
        throw new UnsupportedOperationException("TODO: getProposalsByRequest — placeholder, ver estructura-servicios.docx");
    }

    // =====================================================
    // HELPERS
    // =====================================================

    /** Aplica {@code transition} a cada propuesta PENDING de la solicitud y las persiste. */
    private int resolvePending(Long requestId, Consumer<RescheduleProposal> transition) {
        List<RescheduleProposal> pending = rescheduleProposalPersistencePort
                .findByRequestIdAndStatus(requestId, ProposalStatus.PENDING);
        for (RescheduleProposal proposal : pending) {
            transition.accept(proposal);
            rescheduleProposalPersistencePort.update(proposal);
        }
        return pending.size();
    }

    private RescheduleProposal loadProposal(Long proposalId) {
        return rescheduleProposalPersistencePort.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Propuesta no encontrada: " + proposalId));
    }

    private ServiceRequest loadRequest(Long requestId) {
        return serviceRequestPersistencePort.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada: " + requestId));
    }

    private void requireOwnership(Long ownerId, Long actorId) {
        if (ownerId == null || !ownerId.equals(actorId)) {
            throw new UnauthorizedException("El usuario no es el propietario del recurso");
        }
    }
}
