package com.parosurvivors.serviya.services.infrastructure.mappers;

import com.parosurvivors.serviya.services.application.dto.command.CreateServiceCommand;
import com.parosurvivors.serviya.services.application.dto.command.UpdateServiceCommand;
import com.parosurvivors.serviya.services.domain.Service;
import com.parosurvivors.serviya.services.infrastructure.dto.form.CreateServiceForm;
import com.parosurvivors.serviya.services.infrastructure.dto.form.UpdateServiceForm;
import com.parosurvivors.serviya.services.infrastructure.dto.response.ServiceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper web (MapStruct) de servicios: Form->Command y dominio->Response.
 * Sustituye al antiguo application/mappers/ServiceMapper (que mezclaba dto<->dominio).
 * TODO: completar mapeos donde difieran los nombres de campo.
 */
@Mapper(componentModel = "spring")
public interface ServiceWebMapper {

    @Mapping(target = "offererId", source = "offererId")
    CreateServiceCommand toCommand(CreateServiceForm form, Long offererId);

    @Mapping(target = "serviceId", source = "serviceId")
    UpdateServiceCommand toCommand(UpdateServiceForm form, Long serviceId);

    ServiceResponse toResponse(Service service);

    List<ServiceResponse> toResponses(List<Service> services);
}
