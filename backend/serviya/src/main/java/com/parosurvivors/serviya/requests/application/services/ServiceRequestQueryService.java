package com.parosurvivors.serviya.requests.application.services;

import com.parosurvivors.serviya.requests.application.dto.AdminRequestDetailResponse;
import com.parosurvivors.serviya.requests.application.dto.RequestHistoryResponse;
import com.parosurvivors.serviya.requests.application.dto.ServiceRequestDetailResponse;
import com.parosurvivors.serviya.requests.application.dto.ServiceRequestResponse;
import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestQueryServicePort;
import com.parosurvivors.serviya.requests.application.ports.output.ServiceRequestPersistencePort;
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
    public Page<ServiceRequestResponse> getClientRequests(Long clientId, List<String> statuses, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: getClientRequests — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Page<ServiceRequestResponse> getOffererRequests(Long offererId, List<String> statuses, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: getOffererRequests — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public ServiceRequestDetailResponse getRequestDetailForParty(Long requestId, Long requesterId) {
        throw new UnsupportedOperationException("TODO: getRequestDetailForParty — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public AdminRequestDetailResponse getRequestDetailForAdmin(Long requestId) {
        throw new UnsupportedOperationException("TODO: getRequestDetailForAdmin — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public List<RequestHistoryResponse> getRequestHistory(Long requestId) {
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
