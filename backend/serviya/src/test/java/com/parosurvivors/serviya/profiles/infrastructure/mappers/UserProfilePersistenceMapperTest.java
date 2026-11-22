package com.parosurvivors.serviya.profiles.infrastructure.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.parosurvivors.serviya.profiles.domain.UserProfile;
import com.parosurvivors.serviya.profiles.infrastructure.entities.UserProfileEntity;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/**
 * Lock de B16 (regresión de B10) sobre el camino MapStruct del agregado citado en §8
 * (PATCH /users/me/profile). Antes, el dominio {@link UserProfile} NO tenía campo
 * {@code version}, así que {@code toEntity(dominio)} producía una entidad con
 * {@code version = null} y en el UPDATE Spring Data/Hibernate lo trataba como entidad
 * nueva ("detached entity ... uninitialized version value 'null'"). Con la version en el
 * dominio, MapStruct la auto-mapea en ambos sentidos y el round-trip se conserva.
 */
class UserProfilePersistenceMapperTest {

    private final UserProfilePersistenceMapper mapper =
            Mappers.getMapper(UserProfilePersistenceMapper.class);

    @Test
    void versionRoundTrips_B16() {
        UserProfileEntity entity = new UserProfileEntity();
        entity.setId(10L);
        entity.setVersion(5);
        entity.setUserId(10L);
        entity.setFullName("Ada Lovelace");

        UserProfile domain = mapper.toDomain(entity);
        assertThat(domain.getVersion())
                .as("toDomain debe cargar la version de la fila")
                .isEqualTo(5);

        UserProfileEntity back = mapper.toEntity(domain);
        assertThat(back.getVersion())
                .as("toEntity debe devolver la version para que el save sea un merge, no un insert")
                .isEqualTo(5);
    }
}
