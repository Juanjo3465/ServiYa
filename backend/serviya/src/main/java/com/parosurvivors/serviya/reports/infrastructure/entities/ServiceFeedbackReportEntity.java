package com.parosurvivors.serviya.reports.infrastructure.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "service_feedback_reports")
@Getter
@Setter
public class ServiceFeedbackReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false, unique = true)
    private Long reportId;

    @Column(name = "feedback_id", nullable = false)
    private Long feedbackId;
}
