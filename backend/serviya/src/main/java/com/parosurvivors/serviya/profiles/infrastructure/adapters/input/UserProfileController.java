package com.parosurvivors.serviya.profiles.infrastructure.adapters.input;

import com.parosurvivors.serviya.profiles.application.dto.PatchProfileRequest;
import com.parosurvivors.serviya.profiles.application.dto.UserProfileResponse;
import com.parosurvivors.serviya.profiles.application.ports.input.UserProfileServicePort;
import com.parosurvivors.serviya.profiles.infrastructure.adapters.input.api.UserProfileApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Adaptador de entrada (REST) del perfil personal. Placeholder funcional; documentacion en {@link UserProfileApi}.
 * El requesterId se obtendra del JWT (TODO: integrar Spring Security).
 */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserProfileController implements UserProfileApi {

    private final UserProfileServicePort userProfileService;

    @Override
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfileInfo() {
        return ResponseEntity.ok(userProfileService.getProfileInfo(currentUserId()));
    }

    @Override
    @PatchMapping("/profile")
    public ResponseEntity<Void> patchProfile(@Valid @RequestBody PatchProfileRequest dto) {
        userProfileService.patchProfile(currentUserId(), dto);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/main-address")
    public ResponseEntity<Void> updateMainAddress(@RequestBody Map<String, Long> body) {
        userProfileService.updateMainAddress(currentUserId(), body.get("addressId"));
        return ResponseEntity.noContent().build();
    }

    /** TODO: reemplazar por el id extraido del JWT autenticado. */
    private Long currentUserId() {
        return 0L;
    }
}
