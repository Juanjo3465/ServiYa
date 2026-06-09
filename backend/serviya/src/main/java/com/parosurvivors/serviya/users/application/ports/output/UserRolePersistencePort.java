package com.parosurvivors.serviya.users.application.ports.output;

import java.util.List;

/**
 * Puerto de salida para la tabla puente {@code user_roles}. Trabaja con identificadores
 * planos (no hay objeto de dominio para la asignación); la hidratación de roles de un
 * usuario se compone en el servicio de aplicación junto con {@link RolePersistencePort}.
 */
public interface UserRolePersistencePort {
    void assignRole(Long userId, Integer roleId);
    void removeRole(Long userId, Integer roleId);
    boolean existsByUserIdAndRoleId(Long userId, Integer roleId);
    List<Integer> findRoleIdsByUserId(Long userId);
}
