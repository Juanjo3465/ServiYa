package com.parosurvivors.serviya.users.infrastructure.adapters.input;

import com.parosurvivors.serviya.users.application.ports.input.UserAuthenticationServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserDeletionServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserRoleServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserServicePort;
import com.parosurvivors.serviya.users.infrastructure.dto.response.AuthResponse;
import com.parosurvivors.serviya.shared.security.CurrentUser;
import com.parosurvivors.serviya.users.infrastructure.adapters.input.api.AccountApi;
import com.parosurvivors.serviya.users.infrastructure.dto.form.ChangeEmailForm;
import com.parosurvivors.serviya.users.infrastructure.dto.form.ChangePasswordForm;
import com.parosurvivors.serviya.users.infrastructure.dto.response.RoleResponse;
import com.parosurvivors.serviya.users.infrastructure.mappers.UserWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada (REST) de gestion de cuenta del usuario autenticado. Placeholder funcional.
 * Documentacion en {@link AccountApi}. El requesterId se obtendra del JWT (TODO: integrar Spring Security).
 */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class AccountController implements AccountApi {

    private final UserServicePort userService;
    private final UserDeletionServicePort userDeletionService;
    private final UserRoleServicePort userRoleService;
    /** RF-010/011: adquirir rol re-emite el JWT, por eso pasa por el orquestador de autenticacion. */
    private final UserAuthenticationServicePort authService;
    private final UserWebMapper mapper;

    @Override
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordForm form) {
        userService.changePassword(mapper.toCommand(form, currentUserId()));
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/email")
    public ResponseEntity<Void> changeEmail(@Valid @RequestBody ChangeEmailForm form) {
        userService.changeEmail(mapper.toCommand(form, currentUserId()));
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping
    public ResponseEntity<Void> deleteOwnAccount() {
        userDeletionService.deleteUser(currentUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * RF-010. Devuelve un JWT nuevo con el rol ya incluido: los roles viajan como claim del token, asi
     * que sin re-emitirlo el usuario tendria que volver a iniciar sesion para usarlo. El front guarda
     * este token y obtiene "acceso inmediato".
     */
    @Override
    @PostMapping("/roles/offerer")
    public ResponseEntity<AuthResponse> acquireOffererRole() {
        return ResponseEntity.ok(mapper.toResponse(authService.acquireRole(currentUserId(), "OFFERER")));
    }

    /** RF-011. Ver nota de {@link #acquireOffererRole()} sobre la re-emision del token. */
    @Override
    @PostMapping("/roles/client")
    public ResponseEntity<AuthResponse> acquireClientRole() {
        return ResponseEntity.ok(mapper.toResponse(authService.acquireRole(currentUserId(), "CLIENT")));
    }

    @Override
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> getOwnRoles() {
        return ResponseEntity.ok(mapper.toRoleResponses(userRoleService.getUserRoles(currentUserId())));
    }

    /** Id del usuario autenticado, extraido del JWT por el contexto de seguridad. */
    private Long currentUserId() {
        return CurrentUser.id();
    }
}
