package com.parosurvivors.serviya.admin.infrastructure.mappers;

import com.parosurvivors.serviya.admin.application.dto.command.CreateUserByAdminCommand;
import com.parosurvivors.serviya.admin.application.dto.command.RemoveFeedbackCommand;
import com.parosurvivors.serviya.admin.application.dto.result.UserAdminDetailResult;
import com.parosurvivors.serviya.admin.infrastructure.dto.form.CreateUserByAdminForm;
import com.parosurvivors.serviya.admin.infrastructure.dto.form.RemoveFeedbackForm;
import com.parosurvivors.serviya.admin.infrastructure.dto.response.UserAdminDetailResponse;
import com.parosurvivors.serviya.admin.infrastructure.dto.response.UserSummaryResponse;
import com.parosurvivors.serviya.users.domain.Role;
import com.parosurvivors.serviya.users.domain.User;
import com.parosurvivors.serviya.users.infrastructure.dto.response.RoleResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper web (MapStruct) del panel admin: Form->Command y dominio/Result->Response.
 * Reutiliza RoleResponse del modulo users. TODO: completar mapeos donde difieran los nombres de campo.
 */
@Mapper(componentModel = "spring")
public interface AdminWebMapper {

    @Mapping(target = "adminId", source = "adminId")
    CreateUserByAdminCommand toCommand(CreateUserByAdminForm form, Long adminId);

    @Mapping(target = "adminId", source = "adminId")
    RemoveFeedbackCommand toCommand(RemoveFeedbackForm form, Long adminId);

    UserSummaryResponse toResponse(User user);

    List<UserSummaryResponse> toUserResponses(List<User> users);

    UserAdminDetailResponse toResponse(UserAdminDetailResult result);

    RoleResponse toRoleResponse(Role role);

    List<RoleResponse> toRoleResponses(List<Role> roles);
}
