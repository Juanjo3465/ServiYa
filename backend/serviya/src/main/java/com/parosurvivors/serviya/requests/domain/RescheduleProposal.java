package com.parosurvivors.serviya.requests.domain;

import com.parosurvivors.serviya.shared.exceptions.InvalidStateException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Propuesta de reprogramación de una solicitud. Mapea la tabla {@code reschedule_proposals}.
 * Sólo puede resolverse (aceptar/rechazar) mientras esté {@code PENDING}; las transiciones
 * son idempotentes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleProposal {
    private Long id;
    private Long requestId;
    private String reason;
    private LocalDateTime proposedDate;
    private ProposalStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    // =====================================================
    // BUSINESS METHODS
    // =====================================================

    public boolean isPending() {
        return status == ProposalStatus.PENDING;
    }

    public boolean isResolved() {
        return status == ProposalStatus.ACCEPTED || status == ProposalStatus.REJECTED;
    }

    public void accept() {
        if (status == ProposalStatus.ACCEPTED) {
            return;
        }
        requirePending("aceptar");
        this.status = ProposalStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject() {
        if (status == ProposalStatus.REJECTED) {
            return;
        }
        requirePending("rechazar");
        this.status = ProposalStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    private void requirePending(String action) {
        if (!isPending()) {
            throw new InvalidStateException(
                    "No se puede " + action + " una propuesta en estado " + status);
        }
    }
}
