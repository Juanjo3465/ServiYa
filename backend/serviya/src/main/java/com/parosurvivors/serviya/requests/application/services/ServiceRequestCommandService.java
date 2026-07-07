package com.parosurvivors.serviya.requests.application.services;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.requests.application.dto.command.CreateServiceRequestCommand;
import com.parosurvivors.serviya.requests.application.ports.input.RescheduleProposalServicePort;
import com.parosurvivors.serviya.requests.application.mappers.ServiceRequestCommandMapper;
import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestCommandServicePort;
import com.parosurvivors.serviya.requests.application.ports.output.ServiceRequestPersistencePort;
import com.parosurvivors.serviya.requests.application.ports.output.ServiceRequestReadPort;
import com.parosurvivors.serviya.requests.domain.RequestStatus;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import com.parosurvivors.serviya.services.application.ports.output.ServicePersistencePort;
import com.parosurvivors.serviya.services.domain.Service;
import com.parosurvivors.serviya.shared.events.application.ports.output.DomainEventPublisherPort;
import com.parosurvivors.serviya.shared.events.domain.RequestCreatedEvent;
import com.parosurvivors.serviya.shared.events.domain.RequestStatusChangedEvent;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import com.parosurvivors.serviya.shared.exceptions.UnauthorizedException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de comandos / transiciones de estado de solicitudes (CQRS, módulo 4).
 * Cada transición valida el estado en el dominio, persiste y publica un {@link RequestStatusChangedEvent}
 * para que las métricas de oferente/cliente actualicen sus contadores (vía @TransactionalEventListener,
 * AFTER_COMMIT). La resolución de propuestas se delega en {@link RescheduleProposalServicePort}.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ServiceRequestCommandService implements ServiceRequestCommandServicePort {

    private final ServiceRequestPersistencePort serviceRequestPersistencePort;
    private final ServiceRequestReadPort serviceRequestReadPort;
    private final RescheduleProposalServicePort rescheduleProposalService;
    private final NotificationServicePort notificationServicePort;
    private final ServiceRequestCommandMapper commandMapper;
    private final ServicePersistencePort servicePersistencePort;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public ServiceRequest createRequest(CreateServiceRequestCommand command) {
        Service service = servicePersistencePort.findById(command.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Servicio no encontrado con id: " + command.serviceId()));

        ServiceRequest serviceRequest = commandMapper.toDomain(command);
        serviceRequest.setOffererId(service.getOffererId());
        serviceRequest.setStatus(RequestStatus.PENDING);
        serviceRequest.setRequestedPrice(service.getPriceHourly());
        LocalDateTime now = LocalDateTime.now();
        serviceRequest.setCreatedAt(now);
        serviceRequest.setUpdatedStatusAt(now);
        ServiceRequest saved = serviceRequestPersistencePort.save(serviceRequest);
        // Solicitud original: alimenta requests_sent (cliente) y requests_received (oferente).
        eventPublisher.publish(new RequestCreatedEvent(
                saved.getId(), saved.getClientId(), saved.getOffererId(), saved.getServiceId()));
        // TODO(notif): notificar al oferente la nueva solicitud (RF-061).
        return saved;
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
    @Transactional
    public void acceptRequest(Long requestId, Long offererId) {
        ServiceRequest request = loadRequest(requestId);
        requireOwnership(request.getOffererId(), offererId);
        RequestStatus previous = request.getStatus();
        request.accept(offererId);
        serviceRequestPersistencePort.update(request);
        publishStatusChanged(request, previous);
        // TODO(notif): notificar al cliente que su solicitud fue aceptada (RF-062).
    }

    @Override
    @Transactional
    public void rejectRequest(Long requestId, Long offererId) {
        ServiceRequest request = loadRequest(requestId);
        requireOwnership(request.getOffererId(), offererId);
        RequestStatus previous = request.getStatus();
        request.reject(offererId);
        serviceRequestPersistencePort.update(request);
        publishStatusChanged(request, previous);
        // TODO(notif): notificar al cliente que su solicitud fue rechazada (RF-085).
    }

    @Override
    @Transactional
    public void markAsPresumablyCompleted(Long requestId, Long offererId) {
        ServiceRequest request = loadRequest(requestId);
        requireOwnership(request.getOffererId(), offererId);
        RequestStatus previous = request.getStatus();
        // El oferente declara el servicio prestado: cualquier propuesta PENDING queda sin sentido.
        rescheduleProposalService.cancelPendingProposals(requestId);
        request.markAsPresumablyCompleted(offererId);
        serviceRequestPersistencePort.update(request);
        // PRESUMABLY_COMPLETED no tiene contador de métricas (los listeners lo ignoran); se publica
        // uniformemente para trazabilidad y para habilitar el feedback de ambas partes.
        publishStatusChanged(request, previous);
        // TODO(notif): notificar al cliente que el oferente declaró el servicio prestado (RF-089).
    }

    @Override
    @Transactional
    public void confirmCompletion(Long requestId, Long clientId) {
        ServiceRequest request = loadRequest(requestId);
        requireOwnership(request.getClientId(), clientId);
        RequestStatus previous = request.getStatus();
        request.confirmCompletion(clientId);
        serviceRequestPersistencePort.update(request);
        publishStatusChanged(request, previous);
    }

    @Override
    @Transactional
    public void markAsNotProvided(Long requestId, Long actorId) {
        // Sin validación de acceso aquí: la transición no se expone como endpoint; el control de acceso
        // lo hacen sus llamadores (el orquestador de moderación admin y el mantenimiento como sistema).
        ServiceRequest request = loadRequest(requestId);
        RequestStatus previous = request.getStatus();
        int cancelled = rescheduleProposalService.cancelPendingProposals(requestId);
        if (cancelled > 0) {
            // Había un aviso de reprogramación pendiente: no es incumplimiento, se cancela.
            request.cancel(actorId);
        } else {
            request.markAsNotProvided(actorId);
        }
        serviceRequestPersistencePort.update(request);
        // El estado final es CANCELLED o NOT_PROVIDED según la rama; el evento lleva el real.
        publishStatusChanged(request, previous);
        // TODO(notif): notificar a ambas partes el desenlace (no prestada / cancelada por propuesta pendiente).
    }

    @Override
    @Transactional
    public void cancelRequest(Long requestId, Long userId) {
        ServiceRequest request = loadRequest(requestId);
        requireParticipant(request, userId);
        RequestStatus previous = request.getStatus();
        request.cancel(userId);
        rescheduleProposalService.cancelPendingProposals(requestId);
        serviceRequestPersistencePort.update(request);
        publishStatusChanged(request, previous);
        // TODO(notif): notificar a la contraparte de la cancelación.
    }

    @Override
    @Transactional
    public ServiceRequest rescheduleRequest(Long requestId, LocalDateTime newDate, Long clientId) {
        ServiceRequest request = loadRequest(requestId);
        requireOwnership(request.getClientId(), clientId);

        RequestStatus previous = request.getStatus();
        request.markRescheduled(clientId);
        ServiceRequest replacement = request.rescheduleTo(newDate, RequestStatus.PENDING, clientId);
        // Reprogramación libre: supera cualquier propuesta PENDING del oferente sobre esta solicitud.
        rescheduleProposalService.supersedePendingProposals(requestId);

        serviceRequestPersistencePort.update(request);
        ServiceRequest saved = serviceRequestPersistencePort.save(replacement);
        // Solo la solicitud original cambia de estado (RESCHEDULED); el reemplazo nace PENDING (sin contador).
        publishStatusChanged(request, previous);
        // TODO(notif): notificar al oferente que el cliente reprogramó a una nueva fecha.
        return saved;
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private ServiceRequest loadRequest(Long requestId) {
        return serviceRequestReadPort.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada: " + requestId));
    }

    /**
     * Publica el cambio de estado para que las métricas de oferente/cliente se actualicen por evento
     * (AFTER_COMMIT). El evento es autocontenido: lleva los ids desnormalizados y el estado nuevo.
     */
    private void publishStatusChanged(ServiceRequest request, RequestStatus previous) {
        eventPublisher.publish(new RequestStatusChangedEvent(
                request.getId(),
                request.getClientId(),
                request.getOffererId(),
                request.getServiceId(),
                previous,
                request.getStatus()));
    }

    private void requireOwnership(Long ownerId, Long actorId) {
        if (ownerId == null || !ownerId.equals(actorId)) {
            throw new UnauthorizedException("El usuario no es el propietario del recurso");
        }
    }

    /** El actor debe ser el cliente o el oferente de la solicitud. */
    private void requireParticipant(ServiceRequest request, Long actorId) {
        boolean isParticipant = actorId != null
                && (actorId.equals(request.getClientId()) || actorId.equals(request.getOffererId()));
        if (!isParticipant) {
            throw new UnauthorizedException("El usuario no participa en la solicitud");
        }
    }
}
