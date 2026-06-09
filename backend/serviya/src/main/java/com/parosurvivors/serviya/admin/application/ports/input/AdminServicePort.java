package com.parosurvivors.serviya.admin.application.ports.input;

import com.parosurvivors.serviya.admin.application.dto.command.CreateUserByAdminCommand;
import com.parosurvivors.serviya.admin.application.dto.query.SearchUsersQuery;
import com.parosurvivors.serviya.admin.application.dto.result.UserAdminDetailResult;
import com.parosurvivors.serviya.users.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Puerto de entrada de AdminService — administración de usuarios (contexto backoffice, rol ADMIN).
 * Recibe Command/Query y devuelve dominio (User) o Result; nunca tipos web.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 9).
 */
public interface AdminServicePort {

    User createUserByAdmin(CreateUserByAdminCommand command);

    void grantAdminRole(Long adminId, Long userId);

    Page<User> searchUsers(SearchUsersQuery query, Pageable pageable);

    UserAdminDetailResult getUserAdminDetail(Long userId);

    void banUser(Long adminId, Long userId);

    void unbanUser(Long adminId, Long userId);

    void deleteUser(Long adminId, Long userId);
}
