package com.parosurvivors.serviya.feedback.infrastructure.repositories;

import com.parosurvivors.serviya.feedback.infrastructure.entities.ServiceFeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceFeedbackRepository extends JpaRepository<ServiceFeedbackEntity, Long> {
    Optional<ServiceFeedbackEntity> findByRequestId(Long requestId);
    List<ServiceFeedbackEntity> findByClientId(Long clientId);
}
