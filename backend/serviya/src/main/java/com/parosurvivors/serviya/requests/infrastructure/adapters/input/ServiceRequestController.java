package com.parosurvivors.serviya.requests.infrastructure.adapters.input;

import com.parosurvivors.serviya.requests.application.dto.RequestHistoryResponse;
import com.parosurvivors.serviya.requests.application.dto.ServiceRequestDetailResponse;
import com.parosurvivors.serviya.requests.application.dto.ServiceRequestResponse;
import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestCommandServicePort;
import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestQueryServicePort;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import com.parosurvivors.serviya.requests.infrastructure.adapters.input.api.ServiceRequestApi;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Adaptador de entrada (REST) de solicitudes de servicio. Placeholder funcional;
 * documentacion en {@link ServiceRequestApi}.
 */
@RestController
@RequiredArgsConstructor
public class ServiceRequestController implements ServiceRequestApi {

    private final ServiceRequestQueryServicePort queryService;
    private final ServiceRequestCommandServicePort commandService;

    @Override
    @GetMapping("/api/v1/users/me/client-requests")
    public ResponseEntity<Page<ServiceRequestResponse>> getClientRequests(
            @RequestParam(required = false) List<String> statuses, Pageable pageable) {
        return ResponseEntity.ok(queryService.getClientRequests(currentUserId(), statuses, pageable));
    }

    @Override
    @GetMapping("/api/v1/users/me/offerer-requests")
    public ResponseEntity<Page<ServiceRequestResponse>> getOffererRequests(
            @RequestParam(required = false) List<String> statuses, Pageable pageable) {
        return ResponseEntity.ok(queryService.getOffererRequests(currentUserId(), statuses, pageable));
    }

    @Override
    @GetMapping("/api/v1/service-requests/{id}")
    public ResponseEntity<ServiceRequestDetailResponse> getRequestDetail(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getRequestDetailForParty(id, currentUserId()));
    }

    @Override
    @GetMapping("/api/v1/service-requests/{id}/history")
    public ResponseEntity<List<RequestHistoryResponse>> getRequestHistory(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getRequestHistory(id));
    }

    @Override
    @PostMapping("/api/v1/service-requests")
    public ResponseEntity<ServiceRequest> createRequest(@RequestBody Map<String, String> body) {
        ServiceRequest created = commandService.createRequest(
                currentUserId(),
                Long.valueOf(body.get("serviceId")),
                Long.valueOf(body.get("addressId")),
                LocalDateTime.parse(body.get("scheduledDate")));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
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
    public ResponseEntity<ServiceRequest> rescheduleRequest(@PathVariable Long id,
                                                            @RequestBody Map<String, String> body) {
        ServiceRequest rescheduled = commandService.rescheduleRequest(id, LocalDateTime.parse(body.get("newDate")));
        return ResponseEntity.status(HttpStatus.CREATED).body(rescheduled);
    }

    /** TODO: reemplazar por el id extraido del JWT autenticado. */
    private Long currentUserId() {
        return 0L;
    }
}
