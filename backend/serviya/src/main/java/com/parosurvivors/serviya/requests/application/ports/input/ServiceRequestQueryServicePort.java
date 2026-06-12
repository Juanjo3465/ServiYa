package com.parosurvivors.serviya.requests.application.ports.input;

import com.parosurvivors.serviya.requests.application.dto.item.RequestHistoryItem;
import com.parosurvivors.serviya.requests.application.dto.result.AdminRequestDetailResult;
import com.parosurvivors.serviya.requests.application.dto.result.ServiceRequestDetailResult;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Puerto de entrada de ServiceRequestQueryService (lecturas — CQRS). Las listas devuelven dominio
 * (ServiceRequest); los detalles agregados devuelven Result; el historial devuelve Item. Nunca tipos web.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 4).
 */
public interface ServiceRequestQueryServicePort {

    Page<ServiceRequest> getClientRequests(Long clientId, List<String> statuses, Pageable pageable);

    Page<ServiceRequest> getOffererRequests(Long offererId, List<String> statuses, Pageable pageable);

    //Next two methods area meant for the Agenda feature. Will return future requests, and not completed.
    Page<ServiceRequest> getClientFutureRequests(Long clientId, Pageable pageable);

    Page<ServiceRequest> getOffererFutureRequests(Long offererId, Pageable pageable);

    ServiceRequestDetailResult getRequestDetailForParty(Long requestId, Long requesterId);

    AdminRequestDetailResult getRequestDetailForAdmin(Long requestId);

    List<RequestHistoryItem> getRequestHistory(Long requestId);

    int countClientRequests(Long clientId);

    int countOffererRequests(Long offererId);
}
