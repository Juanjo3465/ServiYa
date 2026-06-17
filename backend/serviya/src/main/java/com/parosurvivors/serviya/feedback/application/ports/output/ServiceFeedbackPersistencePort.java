package com.parosurvivors.serviya.feedback.application.ports.output;

import com.parosurvivors.serviya.feedback.domain.ServiceFeedback;

import java.util.List;
import java.util.Optional;

public interface ServiceFeedbackPersistencePort {
    ServiceFeedback save(ServiceFeedback feedback);
    Optional<ServiceFeedback> findById(Long id);
    Optional<ServiceFeedback> findByRequestId(Long requestId);
    List<ServiceFeedback> findByClientId(Long clientId);
    void deleteById(Long id);
}
