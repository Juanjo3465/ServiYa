package com.parosurvivors.serviya.feedback.infrastructure.repositories;

import com.parosurvivors.serviya.feedback.infrastructure.entities.ClientRatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRatingRepository extends JpaRepository<ClientRatingEntity, Long> {
    Optional<ClientRatingEntity> findByRequestId(Long requestId);
    List<ClientRatingEntity> findByClientId(Long clientId);
    List<ClientRatingEntity> findByOffererId(Long offererId);
}
