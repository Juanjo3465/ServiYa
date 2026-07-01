package com.parosurvivors.serviya.requests.infrastructure.adapters.input;

import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestCommandServicePort;
import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestQueryServicePort;
import com.parosurvivors.serviya.requests.infrastructure.adapters.input.api.ServiceRequestApi;
import com.parosurvivors.serviya.requests.infrastructure.dto.form.CreateServiceRequestForm;
import com.parosurvivors.serviya.requests.infrastructure.dto.form.RescheduleRequestForm;
import com.parosurvivors.serviya.requests.infrastructure.dto.response.RequestHistoryResponse;
import com.parosurvivors.serviya.requests.infrastructure.dto.response.ServiceRequestDetailResponse;
import com.parosurvivors.serviya.requests.infrastructure.dto.response.ServiceRequestResponse;
import com.parosurvivors.serviya.requests.infrastructure.mappers.ServiceRequestWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parosurvivors.serviya.shared.security.CurrentUser;
import java.util.List;

/**
 * Adaptador de entrada (REST) de solicitudes de servicio. Placeholder funcional;
 * documentacion en {@link ServiceRequestApi}. Mapea Form->Command y dominio/Result/Item->Response.
 */
@RestController
@RequiredArgsConstructor
public class ServiceRequestController implements ServiceRequestApi {

    private final ServiceRequestQueryServicePort queryService;
    private final ServiceRequestCommandServicePort commandService;
    private final ServiceRequestWebMapper mapper;

    @Override
    @GetMapping("/api/v1/users/me/client-requests")
    public ResponseEntity<Page<ServiceRequestResponse>> getClientRequests(
            @RequestParam(required = false) List<String> statuses, Pageable pageable) {
        return ResponseEntity.ok(queryService.getClientRequests(currentUserId(), statuses, pageable)
                .map(mapper::toResponse));
    }

    @Override
    @GetMapping("/api/v1/users/me/offerer-requests")
    public ResponseEntity<Page<ServiceRequestResponse>> getOffererRequests(
            @RequestParam(required = false) List<String> statuses, Pageable pageable) {
        return ResponseEntity.ok(queryService.getOffererRequests(currentUserId(), statuses, pageable)
                .map(mapper::toResponse));
    }

    @Override
    @GetMapping("/api/v1/service-requests/{id}")
    public ResponseEntity<ServiceRequestDetailResponse> getRequestDetail(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(queryService.getRequestDetailForParty(id, currentUserId())));
    }

    @Override
    @GetMapping("/api/v1/service-requests/{id}/history")
    public ResponseEntity<List<RequestHistoryResponse>> getRequestHistory(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toHistoryResponses(queryService.getRequestHistory(id)));
    }

    @Override
    @PostMapping("/api/v1/service-requests")
    public ResponseEntity<ServiceRequestResponse> createRequest(@Valid @RequestBody CreateServiceRequestForm form) {
        ServiceRequestResponse response = mapper.toResponse(
                commandService.createRequest(mapper.toCommand(form, currentUserId())));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PostMapping("/api/v1/service-requests/{id}/accept")
    public ResponseEntity<Void> acceptRequest(@PathVariable Long id) {
        commandService.acceptRequest(id, currentUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/api/v1/service-requests/{id}/reject")
    public ResponseEntity<Void> rejectRequest(@PathVariable Long id) {
        commandService.rejectRequest(id, currentUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/api/v1/service-requests/{id}/cancel")
    public ResponseEntity<Void> cancelRequest(@PathVariable Long id) {
        commandService.cancelRequest(id, currentUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/api/v1/service-requests/{id}/mark-completed")
    public ResponseEntity<Void> markCompleted(@PathVariable Long id) {
        commandService.markAsPresumablyCompleted(id, currentUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/api/v1/service-requests/{id}/confirm-completion")
    public ResponseEntity<Void> confirmCompletion(@PathVariable Long id) {
        commandService.confirmCompletion(id, currentUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/api/v1/service-requests/{id}/mark-not-provided")
    public ResponseEntity<Void> markNotProvided(@PathVariable Long id) {
        commandService.markAsNotProvided(id, currentUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/api/v1/service-requests/{id}/reschedule")
    public ResponseEntity<ServiceRequestResponse> rescheduleRequest(@PathVariable Long id,
                                                                    @Valid @RequestBody RescheduleRequestForm form) {
        ServiceRequestResponse response = mapper.toResponse(
                commandService.rescheduleRequest(id, form.newDate()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    

    @Override
    @GetMapping("/api/v1/users/me/client-agenda")
    public ResponseEntity<Page<ServiceRequestResponse>> getClientAgenda(Pageable pageable) {
        return ResponseEntity.ok(queryService.getClientFutureRequests(currentUserId(), pageable)
                .map(mapper::toResponse));
    }

    @Override
    @GetMapping("/api/v1/users/me/offerer-agenda")
    public ResponseEntity<Page<ServiceRequestResponse>> getOffererAgenda(Pageable pageable) {
        return ResponseEntity.ok(queryService.getOffererFutureRequests(currentUserId(), pageable)
                .map(mapper::toResponse));
    }


        private Long currentUserId() {
        return CurrentUser.id();
    } 
}
