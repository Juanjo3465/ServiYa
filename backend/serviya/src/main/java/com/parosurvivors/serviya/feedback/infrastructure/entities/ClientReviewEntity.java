package com.parosurvivors.serviya.feedback.infrastructure.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "client_reviews")
@Getter
@Setter
public class ClientReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true)
    private Long requestId;

    @Column(name = "offerer_id", nullable = false)
    private Long offererId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
