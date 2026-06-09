package com.parosurvivors.serviya.feedback.infrastructure.mappers;

import com.parosurvivors.serviya.feedback.domain.ClientReviewTagCatalog;
import com.parosurvivors.serviya.feedback.infrastructure.entities.ClientReviewTagCatalogEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClientReviewTagCatalogPersistenceMapper {

    ClientReviewTagCatalog toDomain(ClientReviewTagCatalogEntity entity);

    ClientReviewTagCatalogEntity toEntity(ClientReviewTagCatalog domain);
}
