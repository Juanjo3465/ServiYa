package com.parosurvivors.serviya.notifications.infrastructure.mappers;

import com.parosurvivors.serviya.notifications.domain.Notification;
import com.parosurvivors.serviya.notifications.infrastructure.entities.NotificationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationPersistenceMapper {

    Notification toDomain(NotificationEntity entity);

    NotificationEntity toEntity(Notification domain);
}
