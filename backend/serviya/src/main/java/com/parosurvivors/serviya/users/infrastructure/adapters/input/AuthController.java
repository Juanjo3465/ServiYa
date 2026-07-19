package com.parosurvivors.serviya.users.infrastructure.adapters.input;

import com.parosurvivors.serviya.users.application.ports.input.UserAuthenticationServicePort;
import com.parosurvivors.serviya.users.infrastructure.adapters.input.api.AuthApi;
import com.parosurvivors.serviya.users.infrastructure.dto.form.ConfirmPasswordResetForm;
import com.parosurvivors.serviya.users.infrastructure.dto.form.LoginForm;
import com.parosurvivors.serviya.users.infrastructure.dto.form.RegisterUserForm;
import com.parosurvivors.serviya.users.infrastructure.dto.form.RequestPasswordResetForm;
import com.parosurvivors.serviya.users.infrastructure.dto.form.ValidatePasswordResetTokenForm;
import com.parosurvivors.serviya.users.infrastructure.dto.response.AuthResponse;
import com.parosurvivors.serviya.users.infrastructure.mappers.UserWebMapper;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada (REST) de autenticacion. Placeholder funcional: enruta, mapea Form->Command
 * y Result->Response (UserWebMapper) y delega en {@link UserAuthenticationServicePort}.
 * La documentacion vive en {@link AuthApi}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final UserAuthenticationServicePort authService;
    private final UserWebMapper mapper;

    @Override
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterUserForm form) {
        AuthResponse response = mapper.toResponse(authService.register(mapper.toCommand(form)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginForm form) {
        return ResponseEntity.ok(mapper.toResponse(authService.login(mapper.toCommand(form))));
    }

    @Override
    @PostMapping("/password-reset")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody RequestPasswordResetForm form) {
        authService.requestPasswordReset(mapper.toCommand(form));
        return ResponseEntity.accepted().build();
    }

    @Override
    @PostMapping("/password-reset/validate")
    public ResponseEntity<Void> validatePasswordResetToken(
            @Parameter(description = "Token recibido en el enlace del correo", required = true)
            @Valid @RequestBody ValidatePasswordResetTokenForm form) {
        authService.validatePasswordResetToken(form.token());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody ConfirmPasswordResetForm form) {
        authService.confirmPasswordReset(mapper.toCommand(form));
        return ResponseEntity.noContent().build();
    }
}
