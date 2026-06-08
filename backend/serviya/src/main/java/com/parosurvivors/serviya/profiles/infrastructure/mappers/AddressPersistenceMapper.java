package com.parosurvivors.serviya.profiles.infrastructure.mappers;

import com.parosurvivors.serviya.profiles.domain.Address;
import com.parosurvivors.serviya.profiles.infrastructure.entities.AddressEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Conversión dominio &lt;-&gt; entidad para {@link Address}.
 * {@code addressLine} (cifrado en BD: byte[]) se ignora aquí; el cifrado/descifrado
 * lo aplicará un AttributeConverter dedicado (pendiente, ver NOTAS.txt).
 */
@Mapper(componentModel = "spring")
public interface AddressPersistenceMapper {

    @Mapping(target = "addressLine", ignore = true)
    Address toDomain(AddressEntity entity);

    @Mapping(target = "addressLine", ignore = true)
    AddressEntity toEntity(Address domain);
}
