package com.parosurvivors.serviya.requests.infrastructure.adapters.input;

import com.parosurvivors.serviya.requests.application.ports.input.RescheduleProposalServicePort;
import com.parosurvivors.serviya.requests.infrastructure.adapters.input.api.RescheduleProposalApi;
import com.parosurvivors.serviya.requests.infrastructure.dto.form.AcceptProposalForm;
import com.parosurvivors.serviya.requests.infrastructure.dto.form.CreateRescheduleProposalForm;
import com.parosurvivors.serviya.requests.infrastructure.dto.response.RescheduleProposalResponse;
import com.parosurvivors.serviya.requests.infrastructure.dto.response.ServiceRequestResponse;
import com.parosurvivors.serviya.requests.infrastructure.mappers.RescheduleProposalWebMapper;
import com.parosurvivors.serviya.requests.infrastructure.mappers.ServiceRequestWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada (REST) de propuestas de reprogramacion. Placeholder funcional;
 * documentacion en {@link RescheduleProposalApi}. Mapea Form->Command y dominio->Response.
 */
@RestController
@RequiredArgsConstructor
public class RescheduleProposalController implements RescheduleProposalApi {

    private final RescheduleProposalServicePort proposalService;
    private final RescheduleProposalWebMapper mapper;
    private final ServiceRequestWebMapper serviceRequestMapper;

    @Override
    @PostMapping("/api/v1/reschedule-proposals")
    public ResponseEntity<RescheduleProposalResponse> createProposal(
            @Valid @RequestBody CreateRescheduleProposalForm form) {
        RescheduleProposalResponse response = mapper.toResponse(
                proposalService.createProposal(mapper.toCommand(form, currentUserId())));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @GetMapping("/api/v1/users/me/proposals/received")
    public ResponseEntity<List<RescheduleProposalResponse>> getProposalsReceived(
            @RequestParam(required = false) List<String> statuses) {
        return ResponseEntity.ok(mapper.toResponses(
                proposalService.getProposalsForClient(currentUserId(), statuses)));
    }

    @Override
    @GetMapping("/api/v1/users/me/proposals/sent")
    public ResponseEntity<List<RescheduleProposalResponse>> getProposalsSent(
            @RequestParam(required = false) List<String> statuses) {
        return ResponseEntity.ok(mapper.toResponses(
                proposalService.getProposalsByOfferer(currentUserId(), statuses)));
    }

    @Override
    @GetMapping("/api/v1/service-requests/{id}/proposals")
    public ResponseEntity<List<RescheduleProposalResponse>> getProposalsByRequest(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponses(proposalService.getProposalsByRequest(id)));
    }

    @Override
    @PostMapping("/api/v1/reschedule-proposals/{id}/accept")
    public ResponseEntity<ServiceRequestResponse> acceptProposal(@PathVariable Long id,
                                                                 @Valid @RequestBody AcceptProposalForm form) {
        ServiceRequestResponse response = serviceRequestMapper.toResponse(
                proposalService.acceptProposal(id, currentUserId(), form.confirmedDate()));
        return ResponseEntity.ok(response);
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
