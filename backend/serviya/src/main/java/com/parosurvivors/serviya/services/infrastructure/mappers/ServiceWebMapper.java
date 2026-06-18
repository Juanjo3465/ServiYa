package com.parosurvivors.serviya.services.infrastructure.mappers;

import com.parosurvivors.serviya.services.application.dto.command.CreateServiceCommand;
import com.parosurvivors.serviya.services.application.dto.command.UpdateServiceCommand;
import com.parosurvivors.serviya.services.domain.ReviewUser;
import com.parosurvivors.serviya.services.domain.Service;
import com.parosurvivors.serviya.services.domain.ServiceDetail;
import com.parosurvivors.serviya.services.infrastructure.dto.form.CreateServiceForm;
import com.parosurvivors.serviya.services.infrastructure.dto.form.UpdateServiceForm;
import com.parosurvivors.serviya.services.infrastructure.dto.response.ReviewResponse;
import com.parosurvivors.serviya.services.infrastructure.dto.response.ReviewsResponse;
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

    @Mapping(target = "id", source = "review.id")
    @Mapping(target = "requestId", source = "review.requestId")
    @Mapping(target = "comment", source = "review.comment")
    @Mapping(target = "createdAt", source = "review.createdAt")
    @Mapping(target = "userName", source = "user.fullName")
    @Mapping(target = "userPhotoUrl", source = "user.profilePhotoUrl")
    ReviewResponse toReviewResponse(ReviewUser reviewUser);

    default ReviewsResponse toReviewsResponse(List<ReviewUser> reviews) {
        if (reviews == null) return null;
        return new ReviewsResponse(reviews.stream().map(this::toReviewResponse).toList());
    }

    
    ServiceDetailResponse toDetailResponse(ServiceDetail detail);

    List<ServiceResponse> toResponses(List<Service> services);
}
