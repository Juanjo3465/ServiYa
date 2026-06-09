package com.parosurvivors.serviya.feedback.application.mappers;

import com.parosurvivors.serviya.feedback.application.dto.command.SubmitClientFeedbackCommand;
import com.parosurvivors.serviya.feedback.domain.ClientRating;
import com.parosurvivors.serviya.feedback.domain.ClientReview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper de aplicacion (MapStruct) Command -> dominio del feedback de cliente. Capa de aplicacion.
 * PLACEHOLDER: un solo command genera dos entidades de dominio (rating y resena al cliente).
 */
@Mapper(componentModel = "spring")
public interface ClientFeedbackCommandMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ClientRating toRating(SubmitClientFeedbackCommand command);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ClientReview toReview(SubmitClientFeedbackCommand command);
}
