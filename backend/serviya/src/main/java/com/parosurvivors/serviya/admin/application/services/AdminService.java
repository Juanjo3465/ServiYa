package com.parosurvivors.serviya.admin.application.services;

import com.parosurvivors.serviya.admin.application.dto.command.CreateUserByAdminCommand;
import com.parosurvivors.serviya.admin.application.dto.query.SearchUsersQuery;
import com.parosurvivors.serviya.admin.application.dto.result.UserAdminDetailResult;
import com.parosurvivors.serviya.admin.application.ports.input.AdminServicePort;
import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserCreationServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserDeletionServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserRoleServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserServicePort;
import com.parosurvivors.serviya.users.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de AdminServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class AdminService implements AdminServicePort {

    private final UserServicePort userServicePort;
    private final UserCreationServicePort userCreationServicePort;
    private final UserRoleServicePort userRoleServicePort;
    private final UserDeletionServicePort userDeletionServicePort;
    private final NotificationServicePort notificationServicePort;

    @Override
    public User createUserByAdmin(CreateUserByAdminCommand command) {
        throw new UnsupportedOperationException("TODO: createUserByAdmin — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void grantAdminRole(Long adminId, Long userId) {
        throw new UnsupportedOperationException("TODO: grantAdminRole — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Page<User> searchUsers(SearchUsersQuery query, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: searchUsers — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public UserAdminDetailResult getUserAdminDetail(Long userId) {
        throw new UnsupportedOperationException("TODO: getUserAdminDetail — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void banUser(Long adminId, Long userId) {
        throw new UnsupportedOperationException("TODO: banUser — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void unbanUser(Long adminId, Long userId) {
        throw new UnsupportedOperationException("TODO: unbanUser — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void deleteUser(Long adminId, Long userId) {
        throw new UnsupportedOperationException("TODO: deleteUser — placeholder, ver estructura-servicios.docx");
    }
}
