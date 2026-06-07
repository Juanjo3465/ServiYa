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

    RescheduleProposal createProposal(Long requestId, Long offererId, String reason, LocalDateTime proposedDate);

    ServiceRequest acceptProposal(Long proposalId, Long clientId, LocalDateTime confirmedDate);

    void rejectProposal(Long proposalId, Long clientId);

    void cancelProposal(Long proposalId, Long offererId);

    List<RescheduleProposalResponse> getProposalsForClient(Long clientId, List<String> statuses);

    List<RescheduleProposalResponse> getProposalsByOfferer(Long offererId, List<String> statuses);

    List<RescheduleProposalResponse> getProposalsByRequest(Long requestId);
}
