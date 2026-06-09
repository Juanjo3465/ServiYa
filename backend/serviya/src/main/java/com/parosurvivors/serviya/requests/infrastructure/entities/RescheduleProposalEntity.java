package com.parosurvivors.serviya.requests.infrastructure.entities;

import com.parosurvivors.serviya.requests.domain.ProposalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reschedule_proposals")
@Getter
@Setter
public class RescheduleProposalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "proposed_date", nullable = false)
    private LocalDateTime proposedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProposalStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
}
