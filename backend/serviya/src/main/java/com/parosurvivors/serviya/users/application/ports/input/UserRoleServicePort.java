package com.parosurvivors.serviya.users.application.ports.input;

import com.parosurvivors.serviya.users.domain.Role;

import java.util.List;

/**
 * Puerto de entrada de UserRoleService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 1).
 */
public interface UserRoleServicePort {

    List<Role> getUserRoles(int userId);

    boolean hasRole(int userId, String roleName);

    void assignRole(int userId, int roleId);

    void removeRole(int userId, int roleId);

    void acquireRole(int userId, String roleName);
}
