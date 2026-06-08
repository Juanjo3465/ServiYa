package com.parosurvivors.serviya.profiles.infrastructure.mappers;

import com.parosurvivors.serviya.profiles.domain.UserProfile;
import com.parosurvivors.serviya.profiles.infrastructure.entities.UserProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Conversión dominio &lt;-&gt; entidad para {@link UserProfile}.
 * {@code documentNumber} y {@code phoneNumber} (cifrados en BD: byte[]) se ignoran aquí;
 * el cifrado/descifrado lo aplicará un AttributeConverter dedicado (pendiente, ver NOTAS.txt).
 */
@Mapper(componentModel = "spring")
public interface UserProfilePersistenceMapper {

    @Mapping(target = "documentNumber", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    UserProfile toDomain(UserProfileEntity entity);

    @Mapping(target = "documentNumber", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    UserProfileEntity toEntity(UserProfile domain);
}
