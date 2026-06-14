package com.parosurvivors.serviya.services.infrastructure.mappers;

import com.parosurvivors.serviya.metrics.domain.OffererMetrics;
import com.parosurvivors.serviya.profiles.domain.OffererProfile;
import com.parosurvivors.serviya.profiles.domain.OffererProfileSummary;
import com.parosurvivors.serviya.services.application.dto.command.CreateServiceCommand;
import com.parosurvivors.serviya.services.application.dto.command.UpdateServiceCommand;
import com.parosurvivors.serviya.services.domain.Category;
import com.parosurvivors.serviya.services.domain.Service;
import com.parosurvivors.serviya.services.infrastructure.dto.form.CreateServiceForm;
import com.parosurvivors.serviya.services.infrastructure.dto.form.UpdateServiceForm;
import com.parosurvivors.serviya.services.infrastructure.dto.response.OffererProfileResponse;
import com.parosurvivors.serviya.services.infrastructure.dto.response.ServiceDetailResponse;
import com.parosurvivors.serviya.services.infrastructure.dto.response.ServiceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper web (MapStruct) de servicios: Form->Command y dominio->Response.
 * Sustituye al antiguo application/mappers/ServiceMapper (que mezclaba dto<->dominio).
 * TODO: completar mapeos donde difieran los nombres de campo.
 */
@Mapper(componentModel = "spring", uses = {CategoryWebMapper.class})
public interface ServiceWebMapper {

    @Mapping(target = "offererId", source = "offererId")
    CreateServiceCommand toCommand(CreateServiceForm form, Long offererId);

    @Mapping(target = "serviceId", source = "serviceId")
    UpdateServiceCommand toCommand(UpdateServiceForm form, Long serviceId);

    ServiceResponse toResponse(Service service);

    @Mapping(target = "id",       source = "service.id")
    @Mapping(target = "offerer",   source = "offerer")
    @Mapping(target = "category",  source = "category")
    ServiceDetailResponse toDetailResponse(Service service, OffererProfileResponse offerer, Category category);

    @Mapping(target = "userId",            source = "summary.userId")
    @Mapping(target = "fullName",          source = "summary.fullName")
    @Mapping(target = "profilePhotoUrl",   source = "summary.profilePhotoUrl")
    @Mapping(target = "specialty",         source = "summary.specialty")
    @Mapping(target = "averageRating",     source = "summary.averageRating")
    @Mapping(target = "publicDescription", source = "profile.publicDescription")
    OffererProfileResponse toOffererResponse(OffererProfileSummary summary, OffererProfile profile, OffererMetrics metrics);

    List<ServiceResponse> toResponses(List<Service> services);
}
