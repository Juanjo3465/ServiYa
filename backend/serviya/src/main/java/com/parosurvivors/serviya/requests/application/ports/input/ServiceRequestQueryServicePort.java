package com.parosurvivors.serviya.requests.application.ports.input;

import com.parosurvivors.serviya.requests.application.dto.AdminRequestDetailResponse;
import com.parosurvivors.serviya.requests.application.dto.RequestHistoryResponse;
import com.parosurvivors.serviya.requests.application.dto.ServiceRequestDetailResponse;
import com.parosurvivors.serviya.requests.application.dto.ServiceRequestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Puerto de entrada de ServiceRequestQueryService (lecturas — CQRS).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 4).
 */
public interface ServiceRequestQueryServicePort {

    Page<ServiceRequestResponse> getClientRequests(int clientId, List<String> statuses, Pageable pageable);

    Page<ServiceRequestResponse> getOffererRequests(int offererId, List<String> statuses, Pageable pageable);

    ServiceRequestDetailResponse getRequestDetailForParty(int requestId, int requesterId);

    AdminRequestDetailResponse getRequestDetailForAdmin(int requestId);

    List<RequestHistoryResponse> getRequestHistory(int requestId);

    int countClientRequests(int clientId);

    int countOffererRequests(int offererId);
}
