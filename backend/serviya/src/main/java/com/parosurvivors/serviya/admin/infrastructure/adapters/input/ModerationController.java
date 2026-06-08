package com.parosurvivors.serviya.admin.infrastructure.adapters.input;

import com.parosurvivors.serviya.admin.application.dto.CreateReportRequest;
import com.parosurvivors.serviya.admin.application.ports.input.ModerationServicePort;
import com.parosurvivors.serviya.admin.infrastructure.adapters.input.api.ModerationApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada (REST) de moderacion. Placeholder funcional; documentacion en {@link ModerationApi}.
 */
@RestController
@RequiredArgsConstructor
public class ModerationController implements ModerationApi {

    private final ModerationServicePort moderationService;

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
    @PostMapping("/api/v1/admin/reviews/remove")
    public ResponseEntity<Void> removeFeedbackDirectly(@RequestBody CreateReportRequest reportData) {
        moderationService.removeFeedbackDirectly(currentAdminId(), reportData);
        return ResponseEntity.noContent().build();
    }

    /** TODO: reemplazar por el id del admin extraido del JWT autenticado. */
    private Long currentAdminId() {
        return 0L;
    }
}
