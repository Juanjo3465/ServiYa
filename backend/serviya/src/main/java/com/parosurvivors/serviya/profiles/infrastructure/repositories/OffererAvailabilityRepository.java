package com.parosurvivors.serviya.profiles.infrastructure.repositories;

import com.parosurvivors.serviya.profiles.infrastructure.entities.OffererAvailabilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OffererAvailabilityRepository extends JpaRepository<OffererAvailabilityEntity, Long> {
    List<OffererAvailabilityEntity> findByOffererId(Long offererId);

    @Modifying
    @Query("DELETE FROM OffererAvailabilityEntity e WHERE e.offererId = :offererId")
    void deleteByOffererId(@Param("offererId") Long offererId);
}
