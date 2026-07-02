package com.parosurvivors.serviya.feedback.infrastructure.repositories;

import com.parosurvivors.serviya.feedback.infrastructure.entities.ClientFeedbackEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientFeedbackRepository extends JpaRepository<ClientFeedbackEntity, Long> {
    Optional<ClientFeedbackEntity> findByRequestId(Long requestId);
    List<ClientFeedbackEntity> findByClientId(Long clientId);
    List<ClientFeedbackEntity> findByOffererId(Long offererId);
    Page<ClientFeedbackEntity> findByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);
    Page<ClientFeedbackEntity> findByOffererIdOrderByCreatedAtDesc(Long offererId, Pageable pageable);
}
