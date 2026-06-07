package com.parosurvivors.serviya.services.application.mappers;

import com.parosurvivors.serviya.services.application.dto.ServiceRequest;
import com.parosurvivors.serviya.services.application.dto.ServiceResponse;
import com.parosurvivors.serviya.services.domain.Service;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ServiceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Service toDomain(ServiceRequest request);

    ServiceResponse toResponse(Service domain);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "offererId", ignore = true)
    void updateFromRequest(ServiceRequest request, @MappingTarget Service domain);
}
