package com.parosurvivors.serviya.requests.application.services;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestCommandServicePort;
import com.parosurvivors.serviya.requests.application.ports.output.ServiceRequestPersistencePort;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ServiceRequestCommandServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ServiceRequestCommandService implements ServiceRequestCommandServicePort {

    private final ServiceRequestPersistencePort serviceRequestPersistencePort;
    private final NotificationServicePort notificationServicePort;

    @Override
    public ServiceRequest createRequest(Long clientId, Long serviceId, Long addressId, LocalDateTime scheduledDate) {
        throw new UnsupportedOperationException("TODO: createRequest — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public boolean checkServiceAvailability(Long serviceId, LocalDateTime scheduledDate) {
        throw new UnsupportedOperationException("TODO: checkServiceAvailability — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public boolean checkWithinRadius(Long serviceId, Long clientAddressId) {
        throw new UnsupportedOperationException("TODO: checkWithinRadius — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void acceptRequest(Long requestId, Long offererId) {
        throw new UnsupportedOperationException("TODO: acceptRequest — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void rejectRequest(Long requestId, Long offererId) {
        throw new UnsupportedOperationException("TODO: rejectRequest — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void markAsPresumablyCompleted(Long requestId, Long offererId) {
        throw new UnsupportedOperationException("TODO: markAsPresumablyCompleted — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void confirmCompletion(Long requestId, Long clientId) {
        throw new UnsupportedOperationException("TODO: confirmCompletion — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void markAsNotProvided(Long requestId, Long userId) {
        throw new UnsupportedOperationException("TODO: markAsNotProvided — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void cancelRequest(Long requestId, Long userId) {
        throw new UnsupportedOperationException("TODO: cancelRequest — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public ServiceRequest rescheduleRequest(Long requestId, LocalDateTime newDate) {
        throw new UnsupportedOperationException("TODO: rescheduleRequest — placeholder, ver estructura-servicios.docx");
    }
}
