package com.parosurvivors.serviya.requests.application.ports.input;

import com.parosurvivors.serviya.requests.application.dto.command.CreateServiceRequestCommand;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;

import java.time.LocalDateTime;

/**
 * Puerto de entrada de ServiceRequestCommandService (comandos / transiciones de estado — CQRS).
 * createRequest recibe Command y devuelve dominio; las transiciones usan escalares (id + actorId del JWT).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 4).
 */
public interface ServiceRequestCommandServicePort {

    ServiceRequest createRequest(CreateServiceRequestCommand command);

    boolean checkServiceAvailability(Long serviceId, LocalDateTime scheduledDate);

    boolean checkWithinRadius(Long serviceId, Long clientAddressId);

    void acceptRequest(Long requestId, Long offererId);

    void rejectRequest(Long requestId, Long offererId);

    void markAsPresumablyCompleted(Long requestId, Long offererId);

    void confirmCompletion(Long requestId, Long clientId);

    /**
     * Marca la solicitud como no prestada (disputa resuelta o vencimiento sin prestarse). No se expone
     * como endpoint: la invocan el orquestador de moderación (admin) y el mantenimiento programado (sistema);
     * el control de acceso es responsabilidad de esos llamadores, no de esta transición.
     */
    void markAsNotProvided(Long requestId, Long actorId);

    void cancelRequest(Long requestId, Long userId);

    /**
     * Cancela todas las solicitudes activas (PENDING/ACCEPTED) del usuario (como cliente u oferente).
     * No se expone como endpoint: la invoca el orquestador de eliminación de cuenta (UserDeletionService)
     * para no dejar solicitudes huérfanas. El control de acceso es responsabilidad del llamador.
     */
    void cancelActiveRequestsForUser(Long userId);

    ServiceRequest rescheduleRequest(Long requestId, LocalDateTime newDate, Long clientId);
}
