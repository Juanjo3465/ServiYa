package com.parosurvivors.serviya.requests.domain;

/**
 * Estados de una propuesta de reprogramación. Coincide con el ENUM de la columna
 * {@code reschedule_proposals.status}.
 */
public enum ProposalStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
