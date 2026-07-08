package com.parosurvivors.serviya.services.infrastructure.mappers;

import com.parosurvivors.serviya.services.infrastructure.dto.form.CreateServiceAvailabilityForm;
import com.parosurvivors.serviya.services.infrastructure.dto.form.UpdateServiceAvailabilityForm;
import com.parosurvivors.serviya.services.infrastructure.dto.response.ServiceAvailabilityResponse;
import com.parosurvivors.serviya.services.application.dto.command.UpdateServiceAvailabilityCommand;
import com.parosurvivors.serviya.services.domain.ServiceAvailability;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.parosurvivors.serviya.services.application.dto.command.CreateServiceAvailabilityCommand;

@Mapper(componentModel = "spring")

public interface ServiceAvailabilityWebMapper {

    @Mapping(target = "activeStatus", source = "active")
    ServiceAvailabilityResponse toResponse(ServiceAvailability serviceAvailability);

    List<ServiceAvailabilityResponse> toResponses(List<ServiceAvailability> serviceAvailabilities);

    CreateServiceAvailabilityCommand toCommand(CreateServiceAvailabilityForm form, Long serviceId);

    UpdateServiceAvailabilityCommand toCommand(UpdateServiceAvailabilityForm form, Long serviceId);
}
