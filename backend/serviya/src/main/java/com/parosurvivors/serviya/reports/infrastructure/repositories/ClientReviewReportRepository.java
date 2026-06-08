package com.parosurvivors.serviya.reports.infrastructure.repositories;

import com.parosurvivors.serviya.reports.infrastructure.entities.ClientReviewReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientReviewReportRepository extends JpaRepository<ClientReviewReportEntity, Long> {
    Optional<ClientReviewReportEntity> findByReportId(Long reportId);
}
