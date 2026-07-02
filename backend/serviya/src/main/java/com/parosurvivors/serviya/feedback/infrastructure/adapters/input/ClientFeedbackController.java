package com.parosurvivors.serviya.feedback.infrastructure.adapters.input;

import com.parosurvivors.serviya.feedback.application.ports.input.ClientFeedbackServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.ClientFeedbackTagCatalogServicePort;
import com.parosurvivors.serviya.feedback.infrastructure.adapters.input.api.ClientFeedbackApi;
import com.parosurvivors.serviya.feedback.infrastructure.dto.form.SubmitClientFeedbackForm;
import com.parosurvivors.serviya.feedback.infrastructure.dto.response.ClientFeedbackResponse;
import com.parosurvivors.serviya.feedback.infrastructure.dto.response.ClientFeedbackTagResponse;
import com.parosurvivors.serviya.feedback.infrastructure.mappers.ClientFeedbackWebMapper;
import com.parosurvivors.serviya.shared.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ClientFeedbackController implements ClientFeedbackApi {

    private final ClientFeedbackServicePort clientFeedbackService;
    private final ClientFeedbackTagCatalogServicePort clientFeedbackTagCatalogService;
    private final ClientFeedbackWebMapper mapper;

    @Override
    @PostMapping("/api/v1/service-requests/{id}/client-feedback")
    public ResponseEntity<Void> submitClientFeedback(@PathVariable Long id,
                                                     @Valid @RequestBody SubmitClientFeedbackForm form) {
        clientFeedbackService.submitClientFeedback(mapper.toCommand(form, CurrentUser.id(), id));
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/api/v1/service-requests/{id}/client-feedback")
    public ResponseEntity<ClientFeedbackResponse> getClientFeedback(@PathVariable Long id) {
        clientFeedbackService.requireRequestPartyAccess(CurrentUser.id(), id);
        return ResponseEntity.ok(mapper.toResponse(clientFeedbackService.getClientFeedback(id)));
    }

    @Override
    @GetMapping("/api/v1/users/{id}/client-feedback")
    public ResponseEntity<Page<ClientFeedbackResponse>> getClientFeedbackList(@PathVariable Long id,
                                                                             Pageable pageable) {
        clientFeedbackService.requireClientFeedbackListAccess(CurrentUser.id(), id);
        return ResponseEntity.ok(clientFeedbackService.getClientFeedbackList(id, pageable)
                .map(mapper::toResponse));
    }

    @Override
    @GetMapping("/api/v1/offerers/{id}/client-feedback")
    public ResponseEntity<Page<ClientFeedbackResponse>> getClientFeedbackByOfferer(@PathVariable Long id,
                                                                                  Pageable pageable) {
        clientFeedbackService.requireOffererFeedbackAccess(CurrentUser.id(), id);
        return ResponseEntity.ok(clientFeedbackService.getClientFeedbackByOfferer(id, pageable)
                .map(mapper::toResponse));
    }

    @Override
    @GetMapping("/api/v1/client-feedback-tags")
    public ResponseEntity<List<ClientFeedbackTagResponse>> getCatalog() {
        return ResponseEntity.ok(mapper.toTagResponses(clientFeedbackTagCatalogService.getCatalog()));
    }
}
