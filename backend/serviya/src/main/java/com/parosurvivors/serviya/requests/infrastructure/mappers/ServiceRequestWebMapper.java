package com.parosurvivors.serviya.requests.infrastructure.mappers;

import com.parosurvivors.serviya.requests.application.dto.command.CreateServiceRequestCommand;
import com.parosurvivors.serviya.requests.application.dto.item.RequestHistoryItem;
import com.parosurvivors.serviya.requests.application.dto.result.AdminRequestDetailResult;
import com.parosurvivors.serviya.requests.application.dto.result.ServiceRequestDetailResult;
import com.parosurvivors.serviya.requests.domain.ServiceRequest;
import com.parosurvivors.serviya.requests.infrastructure.dto.form.CreateServiceRequestForm;
import com.parosurvivors.serviya.requests.infrastructure.dto.response.AdminRequestDetailResponse;
import com.parosurvivors.serviya.requests.infrastructure.dto.response.RequestHistoryResponse;
import com.parosurvivors.serviya.requests.infrastructure.dto.response.ServiceRequestDetailResponse;
import com.parosurvivors.serviya.requests.infrastructure.dto.response.ServiceRequestResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper web (MapStruct) de solicitudes de servicio: Form->Command y dominio/Result/Item->Response.
 * TODO: completar mapeos donde difieran los nombres de campo.
 */
@Mapper(componentModel = "spring")
public interface ServiceRequestWebMapper {

    @Mapping(target = "clientId", source = "clientId")
    CreateServiceRequestCommand toCommand(CreateServiceRequestForm form, Long clientId);

    ServiceRequestResponse toResponse(ServiceRequest request);

    ServiceRequestDetailResponse toResponse(ServiceRequestDetailResult result);

    AdminRequestDetailResponse toResponse(AdminRequestDetailResult result);

    RequestHistoryResponse toResponse(RequestHistoryItem item);

    List<RequestHistoryResponse> toHistoryResponses(List<RequestHistoryItem> items);
}
