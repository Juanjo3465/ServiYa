package com.parosurvivors.serviya.feedback.infrastructure.repositories;

import com.parosurvivors.serviya.feedback.infrastructure.entities.ServiceReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceReviewRepository extends JpaRepository<ServiceReviewEntity, Long> {
    Optional<ServiceReviewEntity> findByRequestId(Long requestId);
    List<ServiceReviewEntity> findByClientId(Long clientId);
    List<ServiceReviewEntity> findByServiceId(Long serviceId);
    List<ServiceReviewEntity> findTop3ByServiceId(Long serviceId);
}
