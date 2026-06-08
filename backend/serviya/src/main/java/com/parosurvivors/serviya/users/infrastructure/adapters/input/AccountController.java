package com.parosurvivors.serviya.users.infrastructure.adapters.input;

import com.parosurvivors.serviya.users.application.ports.input.UserDeletionServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserRoleServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserServicePort;
import com.parosurvivors.serviya.users.domain.Role;
import com.parosurvivors.serviya.users.infrastructure.adapters.input.api.AccountApi;
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
import java.util.Map;

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

    @Override
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@RequestBody Map<String, String> body) {
        userService.changePassword(currentUserId(), body.get("currentPassword"), body.get("newPassword"));
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/email")
    public ResponseEntity<Void> changeEmail(@RequestBody Map<String, String> body) {
        userService.changeEmail(currentUserId(), body.get("newEmail"));
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping
    public ResponseEntity<Void> deleteOwnAccount() {
        userDeletionService.deleteUser(currentUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/roles/offerer")
    public ResponseEntity<Void> acquireOffererRole() {
        userRoleService.acquireRole(currentUserId(), "OFFERER");
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/roles/client")
    public ResponseEntity<Void> acquireClientRole() {
        userRoleService.acquireRole(currentUserId(), "CLIENT");
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getOwnRoles() {
        return ResponseEntity.ok(userRoleService.getUserRoles(currentUserId()));
    }

    /** TODO: reemplazar por el id extraido del JWT autenticado (Spring Security aun no configurado). */
    private Long currentUserId() {
        return 0L;
    }
}
