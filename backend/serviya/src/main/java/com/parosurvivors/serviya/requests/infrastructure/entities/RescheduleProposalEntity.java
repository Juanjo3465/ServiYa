package com.parosurvivors.serviya.requests.infrastructure.entities;

import com.parosurvivors.serviya.requests.application.dto.item.RescheduleProposalItem;
import com.parosurvivors.serviya.requests.application.dto.result.RescheduleProposalDetailResult;
import com.parosurvivors.serviya.requests.domain.ProposalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Los {@code @SqlResultSetMapping} construyen los read-models de lectura directamente desde las
 * columnas de las queries nativas de {@code RescheduleProposalReadAdapter} (ConstructorResult ->
 * constructor canonico del record; no hay mapeo posterior). El {@code name} de cada columna casa
 * con el alias del SELECT; el ORDEN debe coincidir con los componentes del record.
 */
@Entity
@Table(name = "reschedule_proposals")
@SqlResultSetMappings({
    @SqlResultSetMapping(
        name = "RescheduleProposalItemMapping",
        classes = @ConstructorResult(
            targetClass = RescheduleProposalItem.class,
            columns = {
                @ColumnResult(name = "proposalId", type = Long.class),
                @ColumnResult(name = "status", type = String.class),
                @ColumnResult(name = "originalScheduledDate", type = LocalDateTime.class),
                @ColumnResult(name = "proposedDate", type = LocalDateTime.class),
                @ColumnResult(name = "serviceTitle", type = String.class),
                @ColumnResult(name = "counterpartyName", type = String.class),
                @ColumnResult(name = "counterpartyPhotoUrl", type = String.class),
                @ColumnResult(name = "createdAt", type = LocalDateTime.class)
            })),
    @SqlResultSetMapping(
        name = "RescheduleProposalDetailMapping",
        classes = @ConstructorResult(
            targetClass = RescheduleProposalDetailResult.class,
            columns = {
                @ColumnResult(name = "proposalId", type = Long.class),
                @ColumnResult(name = "status", type = String.class),
                @ColumnResult(name = "reason", type = String.class),
                @ColumnResult(name = "proposedDate", type = LocalDateTime.class),
                @ColumnResult(name = "createdAt", type = LocalDateTime.class),
                @ColumnResult(name = "respondedAt", type = LocalDateTime.class),
                @ColumnResult(name = "requestId", type = Long.class),
                @ColumnResult(name = "requestStatus", type = String.class),
                @ColumnResult(name = "originalScheduledDate", type = LocalDateTime.class),
                @ColumnResult(name = "requestedPrice", type = BigDecimal.class),
                @ColumnResult(name = "addressLabel", type = String.class),
                @ColumnResult(name = "previousRequestId", type = Long.class),
                @ColumnResult(name = "serviceId", type = Long.class),
                @ColumnResult(name = "serviceTitle", type = String.class),
                @ColumnResult(name = "categoryName", type = String.class),
                @ColumnResult(name = "priceHourly", type = BigDecimal.class),
                @ColumnResult(name = "averageDurationMinutes", type = Integer.class),
                @ColumnResult(name = "counterpartyUserId", type = Long.class),
                @ColumnResult(name = "counterpartyName", type = String.class),
                @ColumnResult(name = "counterpartyPhotoUrl", type = String.class)
            }))
})
@Getter
@Setter
public class RescheduleProposalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "offerer_id", nullable = false)
    private Long offererId;

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
