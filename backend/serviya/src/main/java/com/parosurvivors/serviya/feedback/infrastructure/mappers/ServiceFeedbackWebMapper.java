package com.parosurvivors.serviya.feedback.infrastructure.mappers;

import com.parosurvivors.serviya.feedback.application.dto.command.SubmitServiceFeedbackCommand;
import com.parosurvivors.serviya.feedback.application.dto.result.ServiceFeedbackResult;
import com.parosurvivors.serviya.feedback.domain.ServiceReviewTagCatalog;
import com.parosurvivors.serviya.feedback.infrastructure.dto.form.SubmitServiceFeedbackForm;
import com.parosurvivors.serviya.feedback.infrastructure.dto.response.ServiceFeedbackResponse;
import com.parosurvivors.serviya.feedback.infrastructure.dto.response.ServiceReviewTagResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper web (MapStruct) del feedback de servicio: Form->Command y Result/dominio->Response.
 * TODO: completar mapeos donde difieran los nombres de campo.
 */
@Mapper(componentModel = "spring")
public interface ServiceFeedbackWebMapper {

    @Mapping(target = "clientId", source = "clientId")
    @Mapping(target = "requestId", source = "requestId")
    SubmitServiceFeedbackCommand toCommand(SubmitServiceFeedbackForm form, Long clientId, Long requestId);

    ServiceFeedbackResponse toResponse(ServiceFeedbackResult result);

    ServiceReviewTagResponse toResponse(ServiceReviewTagCatalog tag);

    List<ServiceReviewTagResponse> toTagResponses(List<ServiceReviewTagCatalog> tags);
}
