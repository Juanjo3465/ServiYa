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

    /** Feedback recibido por un cliente (RF-047, RF-048), mas reciente primero via Pageable.sort. */
    Page<ClientFeedbackEntity> findByClientId(Long clientId, Pageable pageable);

    /** Feedback de cliente dejado por un oferente, mas reciente primero via Pageable.sort. */
    Page<ClientFeedbackEntity> findByOffererId(Long offererId, Pageable pageable);
}