package com.parosurvivors.serviya.admin.infrastructure.mappers;

import com.parosurvivors.serviya.admin.application.dto.command.CreateUserByAdminCommand;
import com.parosurvivors.serviya.admin.application.dto.command.RemoveFeedbackCommand;
import com.parosurvivors.serviya.admin.application.dto.result.UserAdminDetailResult;
import com.parosurvivors.serviya.admin.infrastructure.dto.form.CreateUserByAdminForm;
import com.parosurvivors.serviya.admin.infrastructure.dto.form.RemoveFeedbackForm;
import com.parosurvivors.serviya.admin.infrastructure.dto.response.UserAdminDetailResponse;
import com.parosurvivors.serviya.admin.infrastructure.dto.response.UserSummaryResponse;
import com.parosurvivors.serviya.metrics.infrastructure.mappers.MetricsWebMapper;
import com.parosurvivors.serviya.users.application.dto.item.UserSummaryItem;
import com.parosurvivors.serviya.users.domain.Role;
import com.parosurvivors.serviya.users.infrastructure.dto.response.RoleResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper web (MapStruct) del panel admin: Form->Command y dominio/Item/Result->Response.
 * Reutiliza RoleResponse del modulo users y las Response de metricas via MetricsWebMapper.
 */
@Mapper(componentModel = "spring", uses = MetricsWebMapper.class)
public interface AdminWebMapper {

    @Mapping(target = "adminId", source = "adminId")
    CreateUserByAdminCommand toCommand(CreateUserByAdminForm form, Long adminId);

    @Mapping(target = "adminId", source = "adminId")
    RemoveFeedbackCommand toCommand(RemoveFeedbackForm form, Long adminId);

    /** Fila del listado y resultado de creacion: el read-model ya trae nombre y foto (null al crear). */
    UserSummaryResponse toResponse(UserSummaryItem item);

    /** Detalle admin: las metricas de oferente/cliente se mapean via MetricsWebMapper (uses). */
    UserAdminDetailResponse toResponse(UserAdminDetailResult result);

    RoleResponse toRoleResponse(Role role);

    List<RoleResponse> toRoleResponses(List<Role> roles);
}
