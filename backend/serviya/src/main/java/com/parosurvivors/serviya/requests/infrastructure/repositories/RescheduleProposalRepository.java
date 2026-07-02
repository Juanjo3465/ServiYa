package com.parosurvivors.serviya.requests.infrastructure.repositories;

import com.parosurvivors.serviya.requests.domain.ProposalStatus;
import com.parosurvivors.serviya.requests.infrastructure.entities.RescheduleProposalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RescheduleProposalRepository extends JpaRepository<RescheduleProposalEntity, Long> {
    List<RescheduleProposalEntity> findByRequestId(Long requestId);
    List<RescheduleProposalEntity> findByRequestIdAndStatus(Long requestId, ProposalStatus status);
    List<RescheduleProposalEntity> findByStatus(ProposalStatus status);
}
