package com.parosurvivors.serviya.requests.infrastructure.entities;

import com.parosurvivors.serviya.requests.domain.RequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_requests")
@Getter
@Setter
public class ServiceRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "previous_request_id", unique = true)
    private Long previousRequestId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "offerer_id", nullable = false)
    private Long offererId;

    @Column(name = "address_id", nullable = false)
    private Long addressId;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "requested_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal requestedPrice;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_status_at")
    private LocalDateTime updatedStatusAt;
}
