package com.parosurvivors.serviya.admin.infrastructure.adapters.input;

import com.parosurvivors.serviya.admin.application.dto.CreateUserRequest;
import com.parosurvivors.serviya.admin.application.dto.UserAdminDetailResponse;
import com.parosurvivors.serviya.admin.application.dto.UserFilterRequest;
import com.parosurvivors.serviya.admin.application.dto.UserSummaryResponse;
import com.parosurvivors.serviya.admin.application.ports.input.AdminServicePort;
import com.parosurvivors.serviya.admin.infrastructure.adapters.input.api.AdminApi;
import com.parosurvivors.serviya.users.application.ports.input.RoleServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserRoleServicePort;
import com.parosurvivors.serviya.users.domain.Role;
import com.parosurvivors.serviya.users.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Adaptador de entrada (REST) de administracion de usuarios y roles. Placeholder funcional;
 * documentacion en {@link AdminApi}.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController implements AdminApi {

    private final AdminServicePort adminService;
    private final UserRoleServicePort userRoleService;
    private final RoleServicePort roleService;

    @Override
    @GetMapping("/users")
    public ResponseEntity<Page<UserSummaryResponse>> searchUsers(UserFilterRequest filters, Pageable pageable) {
        return ResponseEntity.ok(adminService.searchUsers(filters, pageable));
    }

    @Override
    @GetMapping("/users/{id}")
    public ResponseEntity<UserAdminDetailResponse> getUserAdminDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserAdminDetail(id));
    }

    @Override
    @PostMapping("/users")
    public ResponseEntity<User> createUserByAdmin(@RequestParam("role") String roleName,
                                                 @RequestBody CreateUserRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createUserByAdmin(currentAdminId(), dto, roleName));
    }

    @Override
    @PostMapping("/users/{id}/ban")
    public ResponseEntity<Void> banUser(@PathVariable Long id) {
        adminService.banUser(currentAdminId(), id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/users/{id}/unban")
    public ResponseEntity<Void> unbanUser(@PathVariable Long id) {
        adminService.unbanUser(currentAdminId(), id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(currentAdminId(), id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getRoles() {
        return ResponseEntity.ok(roleService.getRoles());
    }

    @Override
    @GetMapping("/users/{id}/roles")
    public ResponseEntity<List<Role>> getUserRoles(@PathVariable Long id) {
        return ResponseEntity.ok(userRoleService.getUserRoles(id));
    }

    @Override
    @PostMapping("/users/{id}/roles")
    public ResponseEntity<Void> assignRole(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        userRoleService.assignRole(id, body.get("roleId"));
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/users/{id}/roles/admin")
    public ResponseEntity<Void> grantAdminRole(@PathVariable Long id) {
        adminService.grantAdminRole(currentAdminId(), id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/users/{id}/roles/{roleId}")
    public ResponseEntity<Void> removeRole(@PathVariable Long id, @PathVariable Long roleId) {
        userRoleService.removeRole(id, roleId);
        return ResponseEntity.noContent().build();
    }

    /** TODO: reemplazar por el id del admin extraido del JWT autenticado. */
    private Long currentAdminId() {
        return 0L;
    }
}
