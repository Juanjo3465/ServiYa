package com.parosurvivors.serviya.feedback.infrastructure.repositories;

import com.parosurvivors.serviya.feedback.infrastructure.entities.ClientReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientReviewRepository extends JpaRepository<ClientReviewEntity, Long> {
    Optional<ClientReviewEntity> findByRequestId(Long requestId);
    List<ClientReviewEntity> findByOffererId(Long offererId);
}
