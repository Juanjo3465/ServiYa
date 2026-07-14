package com.parosurvivors.serviya.admin.application.ports.input;

import com.parosurvivors.serviya.admin.application.dto.command.CreateUserByAdminCommand;
import com.parosurvivors.serviya.admin.application.dto.query.AdminFeedbackSearchQuery;
import com.parosurvivors.serviya.admin.application.dto.result.AdminFeedbackSearchResult;
import com.parosurvivors.serviya.admin.application.dto.result.UserAdminDetailResult;
import com.parosurvivors.serviya.requests.application.dto.result.AdminRequestDetailResult;
import com.parosurvivors.serviya.users.application.dto.item.UserSummaryItem;
import com.parosurvivors.serviya.users.application.dto.query.SearchUsersQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Puerto de entrada de AdminService — administración de usuarios (contexto backoffice, rol ADMIN).
 * Recibe Command/Query y devuelve read-models (Item/Result); nunca tipos web.
 * La búsqueda reutiliza el Query e Item del módulo users (dueño del dato).
 * Ver documents/project-structure/estructura-servicios.docx (módulo 9).
 */
public interface AdminServicePort {

    /** Crea una cuenta con cualquier rol (incl. ADMIN) y devuelve el read-model de listado (sin foto aun). */
    UserSummaryItem createUserByAdmin(CreateUserByAdminCommand command);

    /** Concede cualquier rol (por nombre) a un usuario existente. adminId proviene del JWT. */
    void grantRoleByAdmin(Long adminId, Long userId, String roleName);

    Page<UserSummaryItem> searchUsers(SearchUsersQuery query, Pageable pageable);

    UserAdminDetailResult getUserAdminDetail(Long userId);

    /** Detalle administrativo de una solicitud (ambas partes). Delega en el módulo requests. */
    AdminRequestDetailResult getRequestDetailForAdmin(Long requestId);

    void banUser(Long adminId, Long userId);

    void unbanUser(Long adminId, Long userId);

    void deleteUser(Long adminId, Long userId);

    Page<AdminFeedbackSearchResult> searchFeedback(AdminFeedbackSearchQuery query, Pageable pageable);

    /** Elimina un servicio del marketplace (RF-064). */
    void deleteService(Long serviceId);
}
