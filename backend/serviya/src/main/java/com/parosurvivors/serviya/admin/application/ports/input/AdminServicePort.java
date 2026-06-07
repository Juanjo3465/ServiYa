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

    User createUserByAdmin(Long adminId, CreateUserRequest dto, String roleName);

    void grantAdminRole(Long adminId, Long userId);

    Page<UserSummaryResponse> searchUsers(UserFilterRequest filters, Pageable pageable);

    UserAdminDetailResponse getUserAdminDetail(Long userId);

    void banUser(Long adminId, Long userId);

    void unbanUser(Long adminId, Long userId);

    void deleteUser(Long adminId, Long userId);
}
