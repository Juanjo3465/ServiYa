package com.parosurvivors.serviya.users.infrastructure.adapters.input;

import com.parosurvivors.serviya.users.application.dto.AuthResponse;
import com.parosurvivors.serviya.users.application.dto.RegisterRequest;
import com.parosurvivors.serviya.users.application.ports.input.UserAuthenticationServicePort;
import com.parosurvivors.serviya.users.infrastructure.adapters.input.api.AuthApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Adaptador de entrada (REST) de autenticacion. Placeholder funcional: enruta y delega
 * en {@link UserAuthenticationServicePort}. La documentacion vive en {@link AuthApi}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final UserAuthenticationServicePort authService;

    @Override
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestParam("role") String roleName,
                                                 @Valid @RequestBody RegisterRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(dto, roleName));
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.login(body.get("email"), body.get("password")));
    }

    @Override
    @PostMapping("/password-reset")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody Map<String, String> body) {
        authService.requestPasswordReset(body.get("email"));
        return ResponseEntity.accepted().build();
    }

    @Override
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@RequestBody Map<String, String> body) {
        authService.confirmPasswordReset(body.get("token"), body.get("newPassword"));
        return ResponseEntity.noContent().build();
    }
}
