package com.parosurvivors.serviya.feedback.application.ports.output;

import com.parosurvivors.serviya.feedback.domain.ClientFeedback;

import java.util.List;
import java.util.Optional;

public interface ClientFeedbackPersistencePort {
    ClientFeedback save(ClientFeedback feedback);
    Optional<ClientFeedback> findById(Long id);
    Optional<ClientFeedback> findByRequestId(Long requestId);
    List<ClientFeedback> findByClientId(Long clientId);
    List<ClientFeedback> findByOffererId(Long offererId);
    void deleteById(Long id);
}
