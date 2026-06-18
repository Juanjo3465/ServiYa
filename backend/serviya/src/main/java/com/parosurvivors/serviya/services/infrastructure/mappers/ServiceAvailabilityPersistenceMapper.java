package com.parosurvivors.serviya.services.infrastructure.mappers;

import com.parosurvivors.serviya.services.domain.ServiceAvailability;
import com.parosurvivors.serviya.services.infrastructure.entities.ServiceAvailabilityEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServiceAvailabilityPersistenceMapper {

    ServiceAvailabilityEntity toEntity(ServiceAvailability domain);

    ServiceAvailability toDomain(ServiceAvailabilityEntity entity);
}
