package com.parosurvivors.serviya.requests.infrastructure.adapters.output;

import com.parosurvivors.serviya.requests.application.ports.output.RescheduleProposalPersistencePort;
import com.parosurvivors.serviya.requests.domain.ProposalStatus;
import com.parosurvivors.serviya.requests.domain.RescheduleProposal;
import com.parosurvivors.serviya.requests.infrastructure.entities.RescheduleProposalEntity;
import com.parosurvivors.serviya.requests.infrastructure.mappers.RescheduleProposalPersistenceMapper;
import com.parosurvivors.serviya.requests.infrastructure.repositories.RescheduleProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RescheduleProposalPersistenceAdapter implements RescheduleProposalPersistencePort {

    private final RescheduleProposalRepository repository;
    private final RescheduleProposalPersistenceMapper mapper;

    @Override
    public RescheduleProposal save(RescheduleProposal proposal) {
        RescheduleProposalEntity saved = repository.save(mapper.toEntity(proposal));
        return mapper.toDomain(saved);
    }

    @Override
    public RescheduleProposal update(RescheduleProposal proposal) {
        RescheduleProposalEntity updated = repository.save(mapper.toEntity(proposal));
        return mapper.toDomain(updated);
    }

    @Override
    public Optional<RescheduleProposal> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<RescheduleProposal> findByRequestId(Long requestId) {
        return repository.findByRequestId(requestId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RescheduleProposal> findByStatus(ProposalStatus status) {
        return repository.findByStatus(status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
