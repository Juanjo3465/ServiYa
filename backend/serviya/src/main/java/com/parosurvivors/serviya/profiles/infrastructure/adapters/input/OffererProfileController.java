package com.parosurvivors.serviya.profiles.infrastructure.adapters.input;

import com.parosurvivors.serviya.profiles.application.dto.OffererProfilePublicResponse;
import com.parosurvivors.serviya.profiles.application.dto.OffererProfileSummaryResponse;
import com.parosurvivors.serviya.profiles.application.dto.PatchOffererProfileRequest;
import com.parosurvivors.serviya.profiles.application.ports.input.OffererProfileServicePort;
import com.parosurvivors.serviya.profiles.infrastructure.adapters.input.api.OffererProfileApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada (REST) del perfil de oferente. Placeholder funcional; documentacion en {@link OffererProfileApi}.
 */
@RestController
@RequestMapping("/api/v1/offerers")
@RequiredArgsConstructor
public class OffererProfileController implements OffererProfileApi {

    private final OffererProfileServicePort offererProfileService;

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<OffererProfilePublicResponse> getPublicProfile(@PathVariable Long id) {
        return ResponseEntity.ok(offererProfileService.getPublicProfile(id));
    }

    @Override
    @GetMapping("/{id}/summary")
    public ResponseEntity<OffererProfileSummaryResponse> getProfileSummary(@PathVariable Long id) {
        return ResponseEntity.ok(offererProfileService.getProfileSummary(id));
    }

    @Override
    @PatchMapping("/me")
    public ResponseEntity<Void> patchOffererProfile(@Valid @RequestBody PatchOffererProfileRequest dto) {
        offererProfileService.patchOffererProfile(currentUserId(), dto);
        return ResponseEntity.noContent().build();
    }

    /** TODO: reemplazar por el id extraido del JWT autenticado. */
    private Long currentUserId() {
        return 0L;
    }
}
