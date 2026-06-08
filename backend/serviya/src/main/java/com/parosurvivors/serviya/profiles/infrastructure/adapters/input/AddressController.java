package com.parosurvivors.serviya.profiles.infrastructure.adapters.input;

import com.parosurvivors.serviya.profiles.application.dto.CreateAddressRequest;
import com.parosurvivors.serviya.profiles.application.dto.PatchAddressRequest;
import com.parosurvivors.serviya.profiles.application.ports.input.AddressServicePort;
import com.parosurvivors.serviya.profiles.domain.Address;
import com.parosurvivors.serviya.profiles.infrastructure.adapters.input.api.AddressApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptador de entrada (REST) de direcciones. Placeholder funcional; documentacion en {@link AddressApi}.
 */
@RestController
@RequiredArgsConstructor
public class AddressController implements AddressApi {

    private final AddressServicePort addressService;

    @Override
    @GetMapping("/api/v1/users/me/addresses")
    public ResponseEntity<List<Address>> getUserAddresses() {
        return ResponseEntity.ok(addressService.getUserAddresses(currentUserId()));
    }

    @Override
    @PostMapping("/api/v1/users/me/addresses")
    public ResponseEntity<Address> createAddress(@Valid @RequestBody CreateAddressRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.createAddress(currentUserId(), dto));
    }

    @Override
    @PatchMapping("/api/v1/addresses/{id}")
    public ResponseEntity<Address> updateAddress(@PathVariable Long id,
                                                 @Valid @RequestBody PatchAddressRequest dto) {
        return ResponseEntity.ok(addressService.updateAddress(id, dto));
    }

    @Override
    @DeleteMapping("/api/v1/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        addressService.deleteAddress(currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/api/v1/addresses/verify")
    public ResponseEntity<Map<String, Object>> verifyAddress(@RequestBody Map<String, String> body) {
        String addressLine = body.get("addressLine");
        String city = body.get("city");
        Map<String, Object> result = new HashMap<>();
        result.put("valid", addressService.verifyAddress(addressLine, city));
        result.put("coordinates", addressService.getCoordinates(addressLine, city));
        return ResponseEntity.ok(result);
    }

    /** TODO: reemplazar por el id extraido del JWT autenticado. */
    private Long currentUserId() {
        return 0L;
    }
}
