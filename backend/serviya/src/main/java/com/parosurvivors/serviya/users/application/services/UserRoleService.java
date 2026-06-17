package com.parosurvivors.serviya.users.application.services;

import com.parosurvivors.serviya.shared.exceptions.InvalidStateException;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import com.parosurvivors.serviya.users.application.ports.input.UserRoleServicePort;
import com.parosurvivors.serviya.users.application.ports.output.RolePersistencePort;
import com.parosurvivors.serviya.users.application.ports.output.UserRolePersistencePort;
import com.parosurvivors.serviya.users.domain.Role;
import com.parosurvivors.serviya.users.domain.RoleName;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Gestion de roles de un usuario sobre la tabla puente user_roles. {@code getUserRoles} hidrata
 * los roles (usado por login y por GET /users/me/roles) y {@code acquireRole} asigna un rol
 * publico (CLIENT u OFFERER, nunca ADMIN), usado por el registro y por la auto-asignacion.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class UserRoleService implements UserRoleServicePort {

    private final UserRolePersistencePort userRolePersistencePort;
    private final RolePersistencePort rolePersistencePort;

    @Override
    public List<Role> getUserRoles(Long userId) {
        return userRolePersistencePort.findRoleIdsByUserId(userId).stream()
                .map(roleId -> rolePersistencePort.findById(roleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId)))
                .toList();
    }

    @Override
    public boolean hasRole(Long userId, String roleName) {
        RoleName target = parseRole(roleName);
        return getUserRoles(userId).stream().anyMatch(role -> role.getName() == target);
    }

    @Override
    public void assignRole(Long userId, Long roleId) {
        throw new UnsupportedOperationException("TODO: assignRole — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void removeRole(Long userId, Long roleId) {
        throw new UnsupportedOperationException("TODO: removeRole — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void acquireRole(Long userId, String roleName) {
        RoleName target = parseRole(roleName);
        if (target == RoleName.ADMIN) {
            throw new InvalidStateException("Cannot self-assign the ADMIN role");
        }
        Role role = rolePersistencePort.findByName(target)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + target));
        // assignRole en el adaptador es idempotente (no duplica si ya existe).
        userRolePersistencePort.assignRole(userId, role.getId());
    }

    private RoleName parseRole(String roleName) {
        try {
            return RoleName.valueOf(roleName == null ? "" : roleName.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidStateException("Invalid role: " + roleName);
        }
    }
}
