package com.parosurvivors.serviya.feedback.infrastructure.mappers;

import com.parosurvivors.serviya.feedback.domain.ClientRating;
import com.parosurvivors.serviya.feedback.infrastructure.entities.ClientRatingEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClientRatingPersistenceMapper {

    ClientRating toDomain(ClientRatingEntity entity);

    ClientRatingEntity toEntity(ClientRating domain);
}
