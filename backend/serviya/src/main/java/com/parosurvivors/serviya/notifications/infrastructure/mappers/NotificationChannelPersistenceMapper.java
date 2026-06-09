package com.parosurvivors.serviya.notifications.infrastructure.mappers;

import com.parosurvivors.serviya.notifications.domain.NotificationChannel;
import com.parosurvivors.serviya.notifications.infrastructure.entities.NotificationChannelEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationChannelPersistenceMapper {

    NotificationChannel toDomain(NotificationChannelEntity entity);

    NotificationChannelEntity toEntity(NotificationChannel domain);
}
