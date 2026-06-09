package com.parosurvivors.serviya.feedback.application.mappers;

import com.parosurvivors.serviya.feedback.application.dto.command.SubmitServiceFeedbackCommand;
import com.parosurvivors.serviya.feedback.domain.ServiceRating;
import com.parosurvivors.serviya.feedback.domain.ServiceReview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper de aplicacion (MapStruct) Command -> dominio del feedback de servicio. Capa de aplicacion.
 * PLACEHOLDER: un solo command genera dos entidades de dominio (rating y resena). Lo usaran las
 * fachadas/sub-servicios de feedback; cada parte se persiste solo si viene presente en el command.
 */
@Mapper(componentModel = "spring")
public interface ServiceFeedbackCommandMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ServiceRating toRating(SubmitServiceFeedbackCommand command);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ServiceReview toReview(SubmitServiceFeedbackCommand command);
}
