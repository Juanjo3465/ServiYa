package com.parosurvivors.serviya.profiles.application.ports.input;

import com.parosurvivors.serviya.profiles.application.dto.CoordinatesDTO;
import com.parosurvivors.serviya.profiles.application.dto.CreateAddressRequest;
import com.parosurvivors.serviya.profiles.application.dto.PatchAddressRequest;
import com.parosurvivors.serviya.profiles.domain.Address;

import java.util.List;

/**
 * Puerto de entrada de AddressService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 2).
 */
public interface AddressServicePort {

    List<Address> getUserAddresses(Long userId);

    Address createAddress(Long userId, CreateAddressRequest dto);

    void deleteAddress(Long userId, Long addressId);

    Address updateAddress(Long addressId, PatchAddressRequest dto);

    boolean verifyAddress(String addressLine, String city);

    CoordinatesDTO getCoordinates(String addressLine, String city);
}
