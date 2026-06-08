package com.parosurvivors.serviya.profiles.infrastructure.adapters.input;

import com.parosurvivors.serviya.profiles.application.dto.SlotRequest;
import com.parosurvivors.serviya.profiles.application.ports.input.OffererAvailabilityServicePort;
import com.parosurvivors.serviya.profiles.domain.OffererAvailability;
import com.parosurvivors.serviya.profiles.infrastructure.adapters.input.api.OffererAvailabilityApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada (REST) de la disponibilidad general del oferente. Placeholder funcional;
 * documentacion en {@link OffererAvailabilityApi}.
 */
@RestController
@RequestMapping("/api/v1/offerers/me/availability")
@RequiredArgsConstructor
public class OffererAvailabilityController implements OffererAvailabilityApi {

    private final OffererAvailabilityServicePort offererAvailabilityService;

    @Override
    @GetMapping
    public ResponseEntity<List<OffererAvailability>> getSchedule() {
        return ResponseEntity.ok(offererAvailabilityService.getSchedule(currentUserId()));
    }

    @Override
    @PutMapping
    public ResponseEntity<Void> setSchedule(@Valid @RequestBody List<SlotRequest> slots) {
        offererAvailabilityService.setSchedule(currentUserId(), slots);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/slots/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        offererAvailabilityService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/slots/{id}/activate")
    public ResponseEntity<Void> activateSlot(@PathVariable Long id) {
        offererAvailabilityService.activateSlot(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/slots/{id}/deactivate")
    public ResponseEntity<Void> deactivateSlot(@PathVariable Long id) {
        offererAvailabilityService.deactivateSlot(id);
        return ResponseEntity.noContent().build();
    }

    /** TODO: reemplazar por el id extraido del JWT autenticado. */
    private Long currentUserId() {
        return 0L;
    }
}
