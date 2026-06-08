package com.parosurvivors.serviya.profiles.infrastructure.adapters.input.api;

import com.parosurvivors.serviya.profiles.application.dto.CreateAddressRequest;
import com.parosurvivors.serviya.profiles.application.dto.PatchAddressRequest;
import com.parosurvivors.serviya.profiles.domain.Address;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * Documentacion OpenAPI/Swagger de las direcciones del usuario (modulo 2, seccion 4). RF-009.
 * Ver estructura-endpoints.md.
 */
@Tag(name = "Direcciones", description = "Direcciones del usuario autenticado y verificacion geoespacial")
@SecurityRequirement(name = "bearerAuth")
public interface AddressApi {

    @Operation(summary = "Listar las direcciones propias")
    @ApiResponse(responseCode = "200", description = "Lista de direcciones")
    ResponseEntity<List<Address>> getUserAddresses();

    @Operation(summary = "Crear una nueva direccion",
            description = "La linea de direccion se cifra con AES-256-GCM. RF-009.")
    @ApiResponse(responseCode = "201", description = "Direccion creada")
    ResponseEntity<Address> createAddress(CreateAddressRequest dto);

    @Operation(summary = "Actualizar parcialmente una direccion (PATCH)", description = "Verifica propiedad. RF-009.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Direccion actualizada"),
            @ApiResponse(responseCode = "403", description = "La direccion no pertenece al usuario")
    })
    ResponseEntity<Address> updateAddress(
            @Parameter(description = "Id de la direccion") Long id, PatchAddressRequest dto);

    @Operation(summary = "Eliminar una direccion",
            description = "Verifica propiedad y que no sea la principal activa. RF-009.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Direccion eliminada"),
            @ApiResponse(responseCode = "403", description = "La direccion no pertenece al usuario"),
            @ApiResponse(responseCode = "409", description = "Es la direccion principal activa")
    })
    ResponseEntity<Void> deleteAddress(@Parameter(description = "Id de la direccion") Long id);

    @Operation(summary = "Verificar una direccion y geocodificarla",
            description = "Devuelve { valid, latitude, longitude }. RNF-019.")
    @ApiResponse(responseCode = "200", description = "Resultado de la verificacion")
    ResponseEntity<Map<String, Object>> verifyAddress(
            @Parameter(description = "Cuerpo con 'addressLine' y 'city'") Map<String, String> body);
}
