package com.parosurvivors.serviya.reports.infrastructure.mappers;

import com.parosurvivors.serviya.reports.domain.ServiceReviewReport;
import com.parosurvivors.serviya.reports.infrastructure.entities.ServiceReviewReportEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServiceReviewReportPersistenceMapper {

    ServiceReviewReport toDomain(ServiceReviewReportEntity entity);

    ServiceReviewReportEntity toEntity(ServiceReviewReport domain);
}
