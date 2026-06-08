package com.parosurvivors.serviya.requests.domain;

/**
 * Estados de una solicitud de servicio. Coincide con el ENUM de la columna
 * {@code service_requests.status}.
 */
public enum RequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    COMPLETED,
    RESCHEDULED,
    NOT_PROVIDED;

    /** Estados terminales: ya no admiten más transiciones. */
    public boolean isTerminal() {
        return this == REJECTED
                || this == CANCELLED
                || this == COMPLETED
                || this == RESCHEDULED
                || this == NOT_PROVIDED;
    }
}
