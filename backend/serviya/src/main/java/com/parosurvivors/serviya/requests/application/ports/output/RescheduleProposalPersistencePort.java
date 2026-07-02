package com.parosurvivors.serviya.requests.application.ports.output;

import com.parosurvivors.serviya.requests.domain.ProposalStatus;
import com.parosurvivors.serviya.requests.domain.RescheduleProposal;

import java.util.List;
import java.util.Optional;

public interface RescheduleProposalPersistencePort {
    RescheduleProposal save(RescheduleProposal proposal);
    RescheduleProposal update(RescheduleProposal proposal);
    Optional<RescheduleProposal> findById(Long id);
    List<RescheduleProposal> findByRequestId(Long requestId);
    List<RescheduleProposal> findByRequestIdAndStatus(Long requestId, ProposalStatus status);
    List<RescheduleProposal> findByStatus(ProposalStatus status);
}
