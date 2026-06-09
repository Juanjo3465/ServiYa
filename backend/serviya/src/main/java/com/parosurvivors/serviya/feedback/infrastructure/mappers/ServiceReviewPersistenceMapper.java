package com.parosurvivors.serviya.feedback.infrastructure.mappers;

import com.parosurvivors.serviya.feedback.domain.ServiceReview;
import com.parosurvivors.serviya.feedback.infrastructure.entities.ServiceReviewEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * {@code tagIds} se persiste en la tabla puente service_review_tags y se gestiona aparte
 * en el adaptador; por eso se ignora en este mapper.
 */
@Mapper(componentModel = "spring")
public interface ServiceReviewPersistenceMapper {

    @Mapping(target = "tagIds", ignore = true)
    ServiceReview toDomain(ServiceReviewEntity entity);

    ServiceReviewEntity toEntity(ServiceReview domain);
}
