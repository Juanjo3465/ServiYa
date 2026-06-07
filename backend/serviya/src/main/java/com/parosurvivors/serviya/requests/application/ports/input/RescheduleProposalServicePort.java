package com.parosurvivors.serviya.requests.application.ports.input;

import com.parosurvivors.serviya.requests.application.dto.RescheduleProposalResponse;
import com.parosurvivors.serviya.requests.domain.RescheduleProposal;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Puerto de entrada de RescheduleProposalService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 4).
 */
public interface RescheduleProposalServicePort {

    RescheduleProposal createProposal(int requestId, int offererId, String reason, LocalDateTime proposedDate);

    ServiceRequest acceptProposal(int proposalId, int clientId, LocalDateTime confirmedDate);

    void rejectProposal(int proposalId, int clientId);

    void cancelProposal(int proposalId, int offererId);

    List<RescheduleProposalResponse> getProposalsForClient(int clientId, List<String> statuses);

    List<RescheduleProposalResponse> getProposalsByOfferer(int offererId, List<String> statuses);

    List<RescheduleProposalResponse> getProposalsByRequest(int requestId);
}
