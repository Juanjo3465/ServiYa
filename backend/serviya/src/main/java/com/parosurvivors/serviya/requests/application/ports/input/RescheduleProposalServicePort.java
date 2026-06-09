package com.parosurvivors.serviya.requests.application.ports.input;

import com.parosurvivors.serviya.requests.application.dto.command.CreateRescheduleProposalCommand;
import com.parosurvivors.serviya.requests.domain.RescheduleProposal;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Puerto de entrada de RescheduleProposalService. createProposal recibe Command; las lecturas y demas
 * transiciones devuelven dominio (RescheduleProposal/ServiceRequest). Nunca tipos web.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 4).
 */
public interface RescheduleProposalServicePort {

    RescheduleProposal createProposal(CreateRescheduleProposalCommand command);

    ServiceRequest acceptProposal(Long proposalId, Long clientId, LocalDateTime confirmedDate);

    void rejectProposal(Long proposalId, Long clientId);

    void cancelProposal(Long proposalId, Long offererId);

    List<RescheduleProposal> getProposalsForClient(Long clientId, List<String> statuses);

    List<RescheduleProposal> getProposalsByOfferer(Long offererId, List<String> statuses);

    List<RescheduleProposal> getProposalsByRequest(Long requestId);
}
