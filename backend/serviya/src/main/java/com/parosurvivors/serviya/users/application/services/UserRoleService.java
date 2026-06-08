package com.parosurvivors.serviya.users.application.services;

import com.parosurvivors.serviya.users.application.ports.input.UserRoleServicePort;
import com.parosurvivors.serviya.users.application.ports.output.RolePersistencePort;
import com.parosurvivors.serviya.users.application.ports.output.UserRolePersistencePort;
import com.parosurvivors.serviya.users.domain.Role;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de UserRoleServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class UserRoleService implements UserRoleServicePort {

    private final UserRolePersistencePort userRolePersistencePort;
    private final RolePersistencePort rolePersistencePort;

    @Override
    public List<Role> getUserRoles(Long userId) {
        throw new UnsupportedOperationException("TODO: getUserRoles — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public boolean hasRole(Long userId, String roleName) {
        throw new UnsupportedOperationException("TODO: hasRole — placeholder, ver estructura-servicios.docx");
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
        throw new UnsupportedOperationException("TODO: acquireRole — placeholder, ver estructura-servicios.docx");
    }
}
