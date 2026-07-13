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
    public void assignRole(Long userId, RoleName roleName) {
        // Punto unico de validacion de existencia del rol (por nombre) + duplicado + persistencia.
        Role role = rolePersistencePort.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
        doAssign(userId, role.getId());
    }

    @Override
    public void removeRole(Long userId, Long roleId) {
        userRolePersistencePort.removeRole(userId, toRoleId(roleId));
    }

    /**
     * Punto unico de asignacion: valida duplicado y persiste. La existencia del rol ya la garantizaron las
     * sobrecargas publicas de assignRole (por id o por nombre), asi que ningun llamador la revalida.
     */
    private void doAssign(Long userId, Integer roleId) {
        if (userRolePersistencePort.existsByUserIdAndRoleId(userId, roleId)) {
            throw new InvalidStateException("User " + userId + " already has role id: " + roleId);
        }
        userRolePersistencePort.assignRole(userId, roleId);
    }

    /** Las claves de la tabla roles son INT (Integer); las firmas de entrada usan Long. */
    private Integer toRoleId(Long roleId) {
        if (roleId == null) {
            throw new InvalidStateException("roleId is required");
        }
        return roleId.intValue();
    }

    @Override
    public void acquireRole(Long userId, String roleName) {
        RoleName target = parseRole(roleName);
        if (target == RoleName.ADMIN) {
            throw new InvalidStateException("Cannot self-assign the ADMIN role");
        }
        // Existencia + duplicado + persistencia centralizados en assignRole (por nombre).
        assignRole(userId, target);
    }

    private RoleName parseRole(String roleName) {
        try {
            return RoleName.valueOf(roleName == null ? "" : roleName.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidStateException("Invalid role: " + roleName);
        }
    }
}
