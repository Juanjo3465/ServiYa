package com.parosurvivors.serviya.feedback.infrastructure.mappers;

import com.parosurvivors.serviya.feedback.domain.ServiceReviewTagCatalog;
import com.parosurvivors.serviya.feedback.infrastructure.entities.ServiceReviewTagCatalogEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServiceReviewTagCatalogPersistenceMapper {

    ServiceReviewTagCatalog toDomain(ServiceReviewTagCatalogEntity entity);

    ServiceReviewTagCatalogEntity toEntity(ServiceReviewTagCatalog domain);
}
