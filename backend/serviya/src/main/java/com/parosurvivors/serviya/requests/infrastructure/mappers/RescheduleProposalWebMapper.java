package com.parosurvivors.serviya.requests.infrastructure.mappers;

import com.parosurvivors.serviya.requests.application.dto.command.CreateRescheduleProposalCommand;
import com.parosurvivors.serviya.requests.domain.RescheduleProposal;
import com.parosurvivors.serviya.requests.infrastructure.dto.form.CreateRescheduleProposalForm;
import com.parosurvivors.serviya.requests.infrastructure.dto.response.RescheduleProposalResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper web (MapStruct) de propuestas de reprogramacion: Form->Command y dominio->Response.
 * TODO: completar mapeos donde difieran los nombres de campo.
 */
@Mapper(componentModel = "spring")
public interface RescheduleProposalWebMapper {

    @Mapping(target = "offererId", source = "offererId")
    CreateRescheduleProposalCommand toCommand(CreateRescheduleProposalForm form, Long offererId);

    RescheduleProposalResponse toResponse(RescheduleProposal proposal);

    List<RescheduleProposalResponse> toResponses(List<RescheduleProposal> proposals);
}
