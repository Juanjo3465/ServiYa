package com.parosurvivors.serviya.feedback.infrastructure.mappers;

import com.parosurvivors.serviya.feedback.domain.ServiceRating;
import com.parosurvivors.serviya.feedback.infrastructure.entities.ServiceRatingEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServiceRatingPersistenceMapper {

    ServiceRating toDomain(ServiceRatingEntity entity);

    ServiceRatingEntity toEntity(ServiceRating domain);
}
