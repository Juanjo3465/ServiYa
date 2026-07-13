package com.parosurvivors.serviya.users.application.ports.input;

import com.parosurvivors.serviya.users.domain.Role;
import com.parosurvivors.serviya.users.domain.RoleName;

import java.util.List;

/**
 * Puerto de entrada de UserRoleService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 1).
 */
public interface UserRoleServicePort {

    List<Role> getUserRoles(Long userId);

    boolean hasRole(Long userId, String roleName);

    /** Asigna un rol por nombre. Punto unico de validacion de existencia del rol y de duplicado. */
    void assignRole(Long userId, RoleName roleName);

    void removeRole(Long userId, Long roleId);

    void acquireRole(Long userId, String roleName);
}
