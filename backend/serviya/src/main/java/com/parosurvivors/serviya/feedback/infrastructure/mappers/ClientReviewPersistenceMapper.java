package com.parosurvivors.serviya.feedback.infrastructure.mappers;

import com.parosurvivors.serviya.feedback.domain.ClientReview;
import com.parosurvivors.serviya.feedback.infrastructure.entities.ClientReviewEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * {@code tagIds} se persiste en la tabla puente client_review_tags y se gestiona aparte
 * en el adaptador; por eso se ignora en este mapper.
 */
@Mapper(componentModel = "spring")
public interface ClientReviewPersistenceMapper {

    @Mapping(target = "tagIds", ignore = true)
    ClientReview toDomain(ClientReviewEntity entity);

    ClientReviewEntity toEntity(ClientReview domain);
}
