package com.parosurvivors.serviya.reports.infrastructure.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "service_review_reports")
@Getter
@Setter
public class ServiceReviewReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false, unique = true)
    private Long reportId;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;
}
