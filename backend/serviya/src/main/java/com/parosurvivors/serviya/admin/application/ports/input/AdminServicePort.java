package com.parosurvivors.serviya.admin.application.ports.input;

import com.parosurvivors.serviya.admin.application.dto.CreateUserRequest;
import com.parosurvivors.serviya.admin.application.dto.UserAdminDetailResponse;
import com.parosurvivors.serviya.admin.application.dto.UserFilterRequest;
import com.parosurvivors.serviya.admin.application.dto.UserSummaryResponse;
import com.parosurvivors.serviya.users.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Puerto de entrada de AdminService — administración de usuarios (contexto backoffice, rol ADMIN).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 9).
 */
public interface AdminServicePort {

    User createUserByAdmin(int adminId, CreateUserRequest dto, String roleName);

    void grantAdminRole(int adminId, int userId);

    Page<UserSummaryResponse> searchUsers(UserFilterRequest filters, Pageable pageable);

    UserAdminDetailResponse getUserAdminDetail(int userId);

    void banUser(int adminId, int userId);

    void unbanUser(int adminId, int userId);

    void deleteUser(int adminId, int userId);
}
