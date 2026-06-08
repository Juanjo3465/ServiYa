package com.parosurvivors.serviya.reports.infrastructure.mappers;

import com.parosurvivors.serviya.reports.domain.ClientReviewReport;
import com.parosurvivors.serviya.reports.infrastructure.entities.ClientReviewReportEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClientReviewReportPersistenceMapper {

    ClientReviewReport toDomain(ClientReviewReportEntity entity);

    ClientReviewReportEntity toEntity(ClientReviewReport domain);
}
