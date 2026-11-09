package com.parosurvivors.serviya.bff.infrastructure.mappers;

import com.parosurvivors.serviya.bff.application.dto.OffererPublicProfileView;
import com.parosurvivors.serviya.bff.application.dto.OffererSummaryView;
import com.parosurvivors.serviya.bff.infrastructure.dto.response.OffererProfileSummaryResponse;
import com.parosurvivors.serviya.bff.infrastructure.dto.response.OffererPublicProfileDetailResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BffWebMapper {

    OffererPublicProfileDetailResponse toResponse(OffererPublicProfileView view);

    OffererProfileSummaryResponse toResponse(OffererSummaryView view);
}
