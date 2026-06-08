package com.parosurvivors.serviya.requests.infrastructure.adapters.input;

import com.parosurvivors.serviya.requests.application.dto.RescheduleProposalResponse;
import com.parosurvivors.serviya.requests.application.ports.input.RescheduleProposalServicePort;
import com.parosurvivors.serviya.requests.domain.RescheduleProposal;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import com.parosurvivors.serviya.requests.infrastructure.adapters.input.api.RescheduleProposalApi;
import lombok.RequiredArgsConstructor;
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
 * Adaptador de entrada (REST) de propuestas de reprogramacion. Placeholder funcional;
 * documentacion en {@link RescheduleProposalApi}.
 */
@RestController
@RequiredArgsConstructor
public class RescheduleProposalController implements RescheduleProposalApi {

    private final RescheduleProposalServicePort proposalService;

    @Override
    @PostMapping("/api/v1/reschedule-proposals")
    public ResponseEntity<RescheduleProposal> createProposal(@RequestBody Map<String, String> body) {
        RescheduleProposal created = proposalService.createProposal(
                Long.valueOf(body.get("requestId")),
                currentUserId(),
                body.get("reason"),
                LocalDateTime.parse(body.get("proposedDate")));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    @GetMapping("/api/v1/users/me/proposals/received")
    public ResponseEntity<List<RescheduleProposalResponse>> getProposalsReceived(
            @RequestParam(required = false) List<String> statuses) {
        return ResponseEntity.ok(proposalService.getProposalsForClient(currentUserId(), statuses));
    }

    @Override
    @GetMapping("/api/v1/users/me/proposals/sent")
    public ResponseEntity<List<RescheduleProposalResponse>> getProposalsSent(
            @RequestParam(required = false) List<String> statuses) {
        return ResponseEntity.ok(proposalService.getProposalsByOfferer(currentUserId(), statuses));
    }

    @Override
    @GetMapping("/api/v1/service-requests/{id}/proposals")
    public ResponseEntity<List<RescheduleProposalResponse>> getProposalsByRequest(@PathVariable Long id) {
        return ResponseEntity.ok(proposalService.getProposalsByRequest(id));
    }

    @Override
    @PostMapping("/api/v1/reschedule-proposals/{id}/accept")
    public ResponseEntity<ServiceRequest> acceptProposal(@PathVariable Long id,
                                                        @RequestBody Map<String, String> body) {
        ServiceRequest result = proposalService.acceptProposal(id, currentUserId(),
                LocalDateTime.parse(body.get("confirmedDate")));
        return ResponseEntity.ok(result);
    }

    @Override
    @PostMapping("/api/v1/reschedule-proposals/{id}/reject")
    public ResponseEntity<Void> rejectProposal(@PathVariable Long id) {
        proposalService.rejectProposal(id, currentUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/api/v1/reschedule-proposals/{id}/cancel")
    public ResponseEntity<Void> cancelProposal(@PathVariable Long id) {
        proposalService.cancelProposal(id, currentUserId());
        return ResponseEntity.noContent().build();
    }

    /** TODO: reemplazar por el id extraido del JWT autenticado. */
    private Long currentUserId() {
        return 0L;
    }
}
