package com.parosurvivors.serviya.reports.infrastructure.repositories;

import com.parosurvivors.serviya.reports.infrastructure.entities.ServiceReviewReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceReviewReportRepository extends JpaRepository<ServiceReviewReportEntity, Long> {
    Optional<ServiceReviewReportEntity> findByReportId(Long reportId);
}
