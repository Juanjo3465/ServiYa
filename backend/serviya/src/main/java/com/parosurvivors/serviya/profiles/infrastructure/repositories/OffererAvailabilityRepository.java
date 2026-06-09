package com.parosurvivors.serviya.profiles.infrastructure.repositories;

import com.parosurvivors.serviya.profiles.infrastructure.entities.OffererAvailabilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OffererAvailabilityRepository extends JpaRepository<OffererAvailabilityEntity, Long> {
    List<OffererAvailabilityEntity> findByOffererId(Long offererId);
}
