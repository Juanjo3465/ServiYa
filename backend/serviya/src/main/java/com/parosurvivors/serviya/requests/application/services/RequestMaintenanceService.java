package com.parosurvivors.serviya.requests.application.services;

import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.requests.application.ports.input.RequestMaintenanceServicePort;
import com.parosurvivors.serviya.requests.application.ports.output.RescheduleProposalPersistencePort;
import com.parosurvivors.serviya.requests.application.ports.output.ServiceRequestPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de RequestMaintenanceServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class RequestMaintenanceService implements RequestMaintenanceServicePort {

    private final ServiceRequestPersistencePort serviceRequestPersistencePort;
    private final RescheduleProposalPersistencePort rescheduleProposalPersistencePort;
    private final NotificationServicePort notificationServicePort;

    @Override
    public void rejectExpiredPendingRequests() {
        throw new UnsupportedOperationException("TODO: rejectExpiredPendingRequests — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void markStaleAcceptedAsNotProvided() {
        throw new UnsupportedOperationException("TODO: markStaleAcceptedAsNotProvided — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void rejectExpiredProposals() {
        throw new UnsupportedOperationException("TODO: rejectExpiredProposals — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void finalizeUnconfirmedCompletions() {
        throw new UnsupportedOperationException("TODO: finalizeUnconfirmedCompletions — placeholder, ver estructura-servicios.docx");
    }
}
