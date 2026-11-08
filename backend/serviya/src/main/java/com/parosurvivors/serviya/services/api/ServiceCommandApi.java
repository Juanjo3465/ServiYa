package com.parosurvivors.serviya.services.api;

/**
 * Comandos publicos del modulo services.
 */
public interface ServiceCommandApi {

    /** Desactiva todos los servicios de un oferente (flujo de baja de cuenta). */
    void deactivateAllByOfferer(Long offererId);

    /** Borrado de un servicio por un administrador. */
    void deleteServiceAsAdmin(Long serviceId, Long adminId);
}
