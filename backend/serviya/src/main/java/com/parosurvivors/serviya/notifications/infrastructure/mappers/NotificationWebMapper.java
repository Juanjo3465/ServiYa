package com.parosurvivors.serviya.notifications.infrastructure.mappers;

import com.parosurvivors.serviya.notifications.application.dto.result.NotificationDeliveryResult;
import com.parosurvivors.serviya.notifications.domain.NotificationChannel;
import com.parosurvivors.serviya.notifications.infrastructure.dto.response.NotificationChannelResponse;
import com.parosurvivors.serviya.notifications.infrastructure.dto.response.NotificationDeliveryResponse;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Mapper web (MapStruct) de notificaciones: Result/dominio->Response (solo lecturas).
 * TODO: completar mapeos donde difieran los nombres de campo.
 */
@Mapper(componentModel = "spring")
public interface NotificationWebMapper {

    NotificationDeliveryResponse toResponse(NotificationDeliveryResult result);

    NotificationChannelResponse toResponse(NotificationChannel channel);

    List<NotificationChannelResponse> toChannelResponses(List<NotificationChannel> channels);
}
