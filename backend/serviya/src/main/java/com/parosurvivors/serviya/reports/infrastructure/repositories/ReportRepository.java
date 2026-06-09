package com.parosurvivors.serviya.reports.infrastructure.repositories;

import com.parosurvivors.serviya.reports.domain.ReportStatus;
import com.parosurvivors.serviya.reports.domain.ReportType;
import com.parosurvivors.serviya.reports.infrastructure.entities.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, Long> {
    List<ReportEntity> findByReporterId(Long reporterId);
    List<ReportEntity> findByReportedUserId(Long reportedUserId);
    List<ReportEntity> findByStatus(ReportStatus status);
    List<ReportEntity> findByReportType(ReportType reportType);
    long countByReporterId(Long reporterId);
    long countByReportedUserId(Long reportedUserId);
}
