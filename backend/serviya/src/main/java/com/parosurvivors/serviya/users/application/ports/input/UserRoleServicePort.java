package com.parosurvivors.serviya.users.application.ports.input;

import com.parosurvivors.serviya.users.domain.Role;

import java.util.List;

/**
 * Puerto de entrada de UserRoleService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 1).
 */
public interface UserRoleServicePort {

    List<Role> getUserRoles(Long userId);

    boolean hasRole(Long userId, String roleName);

    void assignRole(Long userId, Long roleId);

    void removeRole(Long userId, Long roleId);

    void acquireRole(Long userId, String roleName);
}
