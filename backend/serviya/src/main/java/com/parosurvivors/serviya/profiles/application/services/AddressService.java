package com.parosurvivors.serviya.profiles.application.services;

import com.parosurvivors.serviya.profiles.application.dto.command.CreateAddressCommand;
import com.parosurvivors.serviya.profiles.application.dto.command.UpdateAddressCommand;
import com.parosurvivors.serviya.profiles.application.dto.result.CoordinatesResult;
import com.parosurvivors.serviya.profiles.application.ports.input.AddressServicePort;
import com.parosurvivors.serviya.profiles.application.ports.output.AddressPersistencePort;
import com.parosurvivors.serviya.profiles.domain.Address;
import java.util.List;
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

    private final AddressPersistencePort addressPersistencePort;

    @Override
    public List<Address> getUserAddresses(Long userId) {
        throw new UnsupportedOperationException("TODO: getUserAddresses — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Address createAddress(CreateAddressCommand command) {
        throw new UnsupportedOperationException("TODO: createAddress — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void deleteAddress(Long userId, Long addressId) {
        throw new UnsupportedOperationException("TODO: deleteAddress — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Address updateAddress(UpdateAddressCommand command) {
        throw new UnsupportedOperationException("TODO: updateAddress — placeholder, ver estructura-servicios.docx");
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
