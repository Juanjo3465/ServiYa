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

    void markAsNotProvided(Long requestId, Long userId);

    void cancelRequest(Long requestId, Long userId);

    ServiceRequest rescheduleRequest(Long requestId, LocalDateTime newDate, Long clientId);
}
