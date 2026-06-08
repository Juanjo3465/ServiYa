package com.parosurvivors.serviya.profiles.application.services;

import com.parosurvivors.serviya.profiles.application.dto.CoordinatesDTO;
import com.parosurvivors.serviya.profiles.application.dto.CreateAddressRequest;
import com.parosurvivors.serviya.profiles.application.dto.PatchAddressRequest;
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
    public Address createAddress(Long userId, CreateAddressRequest dto) {
        throw new UnsupportedOperationException("TODO: createAddress — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void deleteAddress(Long userId, Long addressId) {
        throw new UnsupportedOperationException("TODO: deleteAddress — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public Address updateAddress(Long addressId, PatchAddressRequest dto) {
        throw new UnsupportedOperationException("TODO: updateAddress — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public boolean verifyAddress(String addressLine, String city) {
        throw new UnsupportedOperationException("TODO: verifyAddress — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public CoordinatesDTO getCoordinates(String addressLine, String city) {
        throw new UnsupportedOperationException("TODO: getCoordinates — placeholder, ver estructura-servicios.docx");
    }
}
