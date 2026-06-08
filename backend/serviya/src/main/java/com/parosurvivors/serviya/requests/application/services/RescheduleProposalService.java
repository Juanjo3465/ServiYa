package com.parosurvivors.serviya.requests.application.services;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.requests.application.dto.RescheduleProposalResponse;
import com.parosurvivors.serviya.requests.application.ports.input.RescheduleProposalServicePort;
import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestCommandServicePort;
import com.parosurvivors.serviya.requests.application.ports.output.RescheduleProposalPersistencePort;
import com.parosurvivors.serviya.requests.application.ports.output.ServiceRequestPersistencePort;
import com.parosurvivors.serviya.requests.domain.RescheduleProposal;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de RescheduleProposalServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class RescheduleProposalService implements RescheduleProposalServicePort {

    private final RescheduleProposalPersistencePort rescheduleProposalPersistencePort;
    private final ServiceRequestPersistencePort serviceRequestPersistencePort;
    private final ServiceRequestCommandServicePort serviceRequestCommandServicePort;
    private final NotificationServicePort notificationServicePort;

    @Override
    public RescheduleProposal createProposal(Long requestId, Long offererId, String reason, LocalDateTime proposedDate) {
        throw new UnsupportedOperationException("TODO: createProposal — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public ServiceRequest acceptProposal(Long proposalId, Long clientId, LocalDateTime confirmedDate) {
        throw new UnsupportedOperationException("TODO: acceptProposal — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void rejectProposal(Long proposalId, Long clientId) {
        throw new UnsupportedOperationException("TODO: rejectProposal — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void cancelProposal(Long proposalId, Long offererId) {
        throw new UnsupportedOperationException("TODO: cancelProposal — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public List<RescheduleProposalResponse> getProposalsForClient(Long clientId, List<String> statuses) {
        throw new UnsupportedOperationException("TODO: getProposalsForClient — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public List<RescheduleProposalResponse> getProposalsByOfferer(Long offererId, List<String> statuses) {
        throw new UnsupportedOperationException("TODO: getProposalsByOfferer — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public List<RescheduleProposalResponse> getProposalsByRequest(Long requestId) {
        throw new UnsupportedOperationException("TODO: getProposalsByRequest — placeholder, ver estructura-servicios.docx");
    }
}
