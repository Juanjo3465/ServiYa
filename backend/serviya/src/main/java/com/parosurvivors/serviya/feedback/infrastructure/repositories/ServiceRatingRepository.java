package com.parosurvivors.serviya.feedback.infrastructure.repositories;

import com.parosurvivors.serviya.feedback.infrastructure.entities.ServiceRatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRatingRepository extends JpaRepository<ServiceRatingEntity, Long> {
    Optional<ServiceRatingEntity> findByRequestId(Long requestId);
    List<ServiceRatingEntity> findByClientId(Long clientId);
}
