package com.parosurvivors.serviya.requests.application.services;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.requests.application.dto.command.CreateServiceRequestCommand;
import com.parosurvivors.serviya.requests.application.mappers.ServiceRequestCommandMapper;
import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestCommandServicePort;
import com.parosurvivors.serviya.requests.application.ports.output.ServiceRequestPersistencePort;
import com.parosurvivors.serviya.requests.domain.RequestStatus;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import com.parosurvivors.serviya.services.application.ports.output.ServicePersistencePort;
import com.parosurvivors.serviya.services.domain.Service;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
    private final ServiceRequestCommandMapper commandMapper;
    private final ServicePersistencePort servicePersistencePort;

    @Override
    public ServiceRequest createRequest(CreateServiceRequestCommand command) {
        Service service = servicePersistencePort.findById(command.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Servicio no encontrado con id: " + command.serviceId()));

        ServiceRequest serviceRequest = commandMapper.toDomain(command);
        serviceRequest.setOffererId(service.getOffererId());
        serviceRequest.setStatus(RequestStatus.PENDING);
        serviceRequest.setRequestedPrice(service.getPriceHourly());
        return serviceRequestPersistencePort.save(serviceRequest);
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
