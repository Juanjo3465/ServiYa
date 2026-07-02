package com.parosurvivors.serviya.admin.infrastructure.adapters.input;

import com.parosurvivors.serviya.admin.application.ports.input.ModerationServicePort;
import com.parosurvivors.serviya.admin.infrastructure.adapters.input.api.ModerationApi;
import com.parosurvivors.serviya.admin.infrastructure.dto.form.RemoveFeedbackForm;
import com.parosurvivors.serviya.admin.infrastructure.mappers.AdminWebMapper;
import com.parosurvivors.serviya.shared.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada (REST) de moderacion. Placeholder funcional; documentacion en {@link ModerationApi}.
 * Mapea Form->Command via {@link AdminWebMapper}.
 */
@RestController
@RequiredArgsConstructor
public class ModerationController implements ModerationApi {

    private final ModerationServicePort moderationService;
    private final AdminWebMapper mapper;

    @Override
    @PostMapping("/api/v1/reports/{id}/actions/warn")
    public ResponseEntity<Void> warnUser(@PathVariable Long id) {
        moderationService.warnUser(id, currentAdminId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/api/v1/reports/{id}/actions/ban")
    public ResponseEntity<Void> banUserFromReport(@PathVariable Long id) {
        moderationService.banUserFromReport(id, currentAdminId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/api/v1/reports/{id}/actions/revert-feedback")
    public ResponseEntity<Void> revertFeedback(@PathVariable Long id) {
        moderationService.revertFeedbackFromReport(id, currentAdminId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/api/v1/reports/{id}/actions/close")
    public ResponseEntity<Void> closeReport(@PathVariable Long id) {
        moderationService.closeReport(id, currentAdminId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/api/v1/reports/{id}/actions/mark-not-provided")
    public ResponseEntity<Void> markRequestAsNotProvided(@PathVariable Long id) {
        moderationService.markRequestAsNotProvided(id, currentAdminId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/api/v1/admin/feedback/remove")
    public ResponseEntity<Void> removeFeedbackDirectly(@Valid @RequestBody RemoveFeedbackForm form) {
        moderationService.removeFeedbackDirectly(mapper.toCommand(form, currentAdminId()));
        return ResponseEntity.noContent().build();
    }

    /** TODO: reemplazar por el id del admin extraido del JWT autenticado. */
    private Long currentAdminId() {
        return CurrentUser.id();
    }
}
