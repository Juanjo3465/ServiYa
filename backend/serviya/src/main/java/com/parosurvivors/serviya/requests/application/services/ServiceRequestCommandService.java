package com.parosurvivors.serviya.requests.application.services;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.requests.application.dto.command.CreateServiceRequestCommand;
import com.parosurvivors.serviya.requests.application.ports.input.RescheduleProposalServicePort;
import com.parosurvivors.serviya.requests.application.mappers.ServiceRequestCommandMapper;
import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestCommandServicePort;
import com.parosurvivors.serviya.requests.application.ports.output.ServiceRequestPersistencePort;
import com.parosurvivors.serviya.requests.domain.RequestStatus;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import com.parosurvivors.serviya.services.application.ports.output.ServicePersistencePort;
import com.parosurvivors.serviya.services.domain.Service;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import com.parosurvivors.serviya.shared.exceptions.UnauthorizedException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de comandos / transiciones de estado de solicitudes (CQRS, módulo 4).
 * Implementadas: reprogramación libre + las transiciones que resuelven propuestas PENDING
 * (cancelar, presuntamente-completar, confirmar, no-prestada y crear). El resto sigue placeholder.
 * La resolución de propuestas se delega en {@link RescheduleProposalServicePort} (centralizada).
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ServiceRequestCommandService implements ServiceRequestCommandServicePort {

    private final ServiceRequestPersistencePort serviceRequestPersistencePort;
    private final RescheduleProposalServicePort rescheduleProposalService;
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
    @Transactional
    public void markAsPresumablyCompleted(Long requestId, Long offererId) {
        ServiceRequest request = loadRequest(requestId);
        requireOwnership(request.getOffererId(), offererId);
        // El oferente declara el servicio prestado: cualquier propuesta PENDING queda sin sentido.
        rescheduleProposalService.cancelPendingProposals(requestId);
        request.markAsPresumablyCompleted(offererId);
        serviceRequestPersistencePort.update(request);
        // TODO(event): disparar habilitación de feedback para cliente y oferente.
    }

    @Override
    @Transactional
    public void confirmCompletion(Long requestId, Long clientId) {
        ServiceRequest request = loadRequest(requestId);
        requireOwnership(request.getClientId(), clientId);
        request.confirmCompletion(clientId);
        serviceRequestPersistencePort.update(request);
        // TODO(event): publicar evento de completado para métricas.
    }

    @Override
    @Transactional
    public void markAsNotProvided(Long requestId, Long userId) {
        // Sin verificación de propiedad: lo ejecuta un admin o el sistema (fecha vencida sin propuesta,
        // o disputa resuelta). El control de acceso (rol admin) es responsabilidad de la seguridad.
        ServiceRequest request = loadRequest(requestId);
        int cancelled = rescheduleProposalService.cancelPendingProposals(requestId);
        if (cancelled > 0) {
            // Había un aviso de reprogramación pendiente: no es incumplimiento, se cancela.
            request.cancel(userId);
        } else {
            request.markAsNotProvided(userId);
        }
        serviceRequestPersistencePort.update(request);
        // TODO(event): publicar evento para métricas de incumplimiento.
    }

    @Override
    @Transactional
    public void cancelRequest(Long requestId, Long userId) {
        ServiceRequest request = loadRequest(requestId);
        requireParticipant(request, userId);
        request.cancel(userId);
        rescheduleProposalService.cancelPendingProposals(requestId);
        serviceRequestPersistencePort.update(request);
        // TODO(notif): notificar a la contraparte de la cancelación.
    }

    @Override
    @Transactional
    public ServiceRequest rescheduleRequest(Long requestId, LocalDateTime newDate, Long clientId) {
        ServiceRequest request = loadRequest(requestId);
        requireOwnership(request.getClientId(), clientId);

        request.markRescheduled(clientId);
        ServiceRequest replacement = request.rescheduleTo(newDate, RequestStatus.PENDING, clientId);
        // Reprogramación libre: supera cualquier propuesta PENDING del oferente sobre esta solicitud.
        rescheduleProposalService.supersedePendingProposals(requestId);

        serviceRequestPersistencePort.update(request);
        ServiceRequest saved = serviceRequestPersistencePort.save(replacement);
        // TODO(notif): notificar al oferente que el cliente reprogramó a una nueva fecha.
        return saved;
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private ServiceRequest loadRequest(Long requestId) {
        return serviceRequestPersistencePort.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada: " + requestId));
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
