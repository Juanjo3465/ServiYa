package com.parosurvivors.serviya.feedback.application.ports.output;

import com.parosurvivors.serviya.feedback.domain.ClientFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ClientFeedbackPersistencePort {
    ClientFeedback save(ClientFeedback feedback);
    Optional<ClientFeedback> findById(Long id);
    Optional<ClientFeedback> findByRequestId(Long requestId);
    List<ClientFeedback> findByClientId(Long clientId);
    List<ClientFeedback> findByOffererId(Long offererId);

    /** Feedback recibido por un cliente, paginado (RF-047, RF-048). */
    Page<ClientFeedback> findByClientId(Long clientId, Pageable pageable);

    /** Feedback de cliente dejado por un oferente, paginado. */
    Page<ClientFeedback> findByOffererId(Long offererId, Pageable pageable);

    void deleteById(Long id);
}