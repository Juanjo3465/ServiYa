package com.parosurvivors.serviya.requests.application.ports.input;

import com.parosurvivors.serviya.requests.domain.ServiceRequest;

import java.time.LocalDateTime;

/**
 * Puerto de entrada de ServiceRequestCommandService (comandos / transiciones de estado — CQRS).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 4).
 */
public interface ServiceRequestCommandServicePort {

    ServiceRequest createRequest(int clientId, int serviceId, int addressId, LocalDateTime scheduledDate);

    boolean checkServiceAvailability(int serviceId, LocalDateTime scheduledDate);

    boolean checkWithinRadius(int serviceId, int clientAddressId);

    void acceptRequest(int requestId, int offererId);

    void rejectRequest(int requestId, int offererId);

    void markAsPresumablyCompleted(int requestId, int offererId);

    void confirmCompletion(int requestId, int clientId);

    void markAsNotProvided(int requestId, int userId);

    void cancelRequest(int requestId, int userId);

    ServiceRequest rescheduleRequest(int requestId, LocalDateTime newDate);
}
