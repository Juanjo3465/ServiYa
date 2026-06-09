package com.parosurvivors.serviya.notifications.infrastructure.mappers;

import com.parosurvivors.serviya.notifications.domain.NotificationDelivery;
import com.parosurvivors.serviya.notifications.infrastructure.entities.NotificationDeliveryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationDeliveryPersistenceMapper {

    NotificationDelivery toDomain(NotificationDeliveryEntity entity);

    NotificationDeliveryEntity toEntity(NotificationDelivery domain);
}
