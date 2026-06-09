package com.parosurvivors.serviya.feedback.infrastructure.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_ratings")
@Getter
@Setter
public class ServiceRatingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true)
    private Long requestId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
