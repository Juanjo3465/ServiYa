package com.parosurvivors.serviya.profiles.application.services;

import com.parosurvivors.serviya.profiles.application.dto.command.CreateAddressCommand;
import com.parosurvivors.serviya.profiles.application.dto.command.UpdateAddressCommand;
import com.parosurvivors.serviya.profiles.application.dto.result.CoordinatesResult;
import com.parosurvivors.serviya.profiles.application.mappers.AddressCommandMapper;
import com.parosurvivors.serviya.profiles.application.ports.input.AddressServicePort;
import com.parosurvivors.serviya.profiles.application.ports.output.AddressPersistencePort;
import com.parosurvivors.serviya.profiles.application.ports.output.UserProfilePersistencePort;
import com.parosurvivors.serviya.profiles.domain.Address;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.parosurvivors.serviya.services.domain.Service;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de AddressServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class AddressService implements AddressServicePort {

    private final UserProfilePersistencePort profilePersistencePort;
    private final AddressPersistencePort persistencePort;
    private final AddressCommandMapper commandMapper;

    @Override
    public List<Address> getUserAddresses(Long userId) {
        return new ArrayList<>(persistencePort.findByUserId(userId));
    }

    @Override
    public Address createAddress(CreateAddressCommand command) {
        LocalDateTime now = LocalDateTime.now();
        Address address = commandMapper.toDomain(command);
        address.setCreatedAt(now);
        return persistencePort.save(address);
    }

    @Override
    public void deleteAddress(Long addressId) {
        if (persistencePort.findById(addressId).isEmpty()) {
            throw new ResourceNotFoundException("Dirección no encontrada con id: " + addressId);
        }
        persistencePort.deleteById(addressId);
    }

    @Override
    public Address updateAddress(UpdateAddressCommand command) {
        Address address = persistencePort.findById(command.addressId())
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada con id: " + command.addressId()));

        commandMapper.updateFromCommand(command, address);

        return persistencePort.update(address);
    }

    @Override
    public boolean verifyAddress(String addressLine, String city) {
        throw new UnsupportedOperationException("TODO: verifyAddress — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public CoordinatesResult getCoordinates(String addressLine, String city) {
        throw new UnsupportedOperationException("TODO: getCoordinates — placeholder, ver estructura-servicios.docx");
    }
}
