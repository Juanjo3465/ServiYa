package com.parosurvivors.serviya.services.infrastructure.repositories;

import com.parosurvivors.serviya.services.infrastructure.entities.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    List<ServiceEntity> findByOffererId(Long offererId);
}
