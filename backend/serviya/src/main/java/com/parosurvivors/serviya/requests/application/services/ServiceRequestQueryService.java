package com.parosurvivors.serviya.requests.application.services;

import com.parosurvivors.serviya.requests.application.dto.item.RequestHistoryItem;
import com.parosurvivors.serviya.requests.application.dto.result.AdminRequestDetailResult;
import com.parosurvivors.serviya.requests.application.dto.result.ServiceRequestDetailResult;
import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestQueryServicePort;
import com.parosurvivors.serviya.requests.application.ports.output.ServiceRequestPersistencePort;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ServiceRequestQueryServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ServiceRequestQueryService implements ServiceRequestQueryServicePort {

    private final ServiceRequestPersistencePort serviceRequestPersistencePort;

    @Override
    public Page<ServiceRequest> getClientRequests(Long clientId, List<String> statuses, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: getClientRequests — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Page<ServiceRequest> getOffererRequests(Long offererId, List<String> statuses, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: getOffererRequests — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Page<ServiceRequest> getClientFutureRequests(Long clientId, Pageable pageable) {
        //throw new UnsupportedOperationException("TODO: getClientFutureRequests — placeholder, ver estructura-servicios.docx");
        return serviceRequestPersistencePort.findClientFutureRequests(clientId, List.of("ACCEPTED", "PENDING"), pageable);
    }

    @Override
    public Page<ServiceRequest> getOffererFutureRequests(Long offererId, Pageable pageable) {
        //throw new UnsupportedOperationException("TODO: getOffererFutureRequests — placeholder, ver estructura-servicios.docx");
        return serviceRequestPersistencePort.findOffererFutureRequests(offererId, List.of("ACCEPTED"), pageable);
    }

    @Override
    public ServiceRequestDetailResult getRequestDetailForParty(Long requestId, Long requesterId) {
        throw new UnsupportedOperationException("TODO: getRequestDetailForParty — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public AdminRequestDetailResult getRequestDetailForAdmin(Long requestId) {
        throw new UnsupportedOperationException("TODO: getRequestDetailForAdmin — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public List<RequestHistoryItem> getRequestHistory(Long requestId) {
        throw new UnsupportedOperationException("TODO: getRequestHistory — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public int countClientRequests(Long clientId) {
        throw new UnsupportedOperationException("TODO: countClientRequests — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public int countOffererRequests(Long offererId) {
        throw new UnsupportedOperationException("TODO: countOffererRequests — placeholder, ver estructura-servicios.docx");
    }
}
