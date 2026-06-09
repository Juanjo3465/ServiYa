package com.parosurvivors.serviya.requests.infrastructure.repositories;

import com.parosurvivors.serviya.requests.domain.RequestStatus;
import com.parosurvivors.serviya.requests.infrastructure.entities.ServiceRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequestEntity, Long> {
    List<ServiceRequestEntity> findByClientId(Long clientId);
    List<ServiceRequestEntity> findByOffererId(Long offererId);
    List<ServiceRequestEntity> findByServiceId(Long serviceId);
    List<ServiceRequestEntity> findByStatus(RequestStatus status);
    Optional<ServiceRequestEntity> findByPreviousRequestId(Long previousRequestId);
    long countByClientId(Long clientId);
    long countByOffererId(Long offererId);
}
