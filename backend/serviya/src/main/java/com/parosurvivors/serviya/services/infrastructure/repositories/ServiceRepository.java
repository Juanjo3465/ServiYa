package com.parosurvivors.serviya.services.infrastructure.repositories;

import com.parosurvivors.serviya.services.infrastructure.entities.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
}
