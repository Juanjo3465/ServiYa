package com.parosurvivors.serviya.services.infrastructure.adapters.input;

import com.parosurvivors.serviya.services.application.dto.ServiceRequest;
import com.parosurvivors.serviya.services.application.dto.ServiceResponse;
import com.parosurvivors.serviya.services.application.ports.input.MarketplaceServicePort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "API de gestión de servicios del marketplace")
public class ServiceController {
    
    private final MarketplaceServicePort marketplaceService;

    @PostMapping
    @Operation(summary = "Crear un nuevo servicio")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Servicio creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<ServiceResponse> create(@Valid @RequestBody ServiceRequest request) {
        ServiceResponse response = marketplaceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un servicio por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Servicio encontrado"),
        @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    })
    public ResponseEntity<ServiceResponse> getById(
            @Parameter(description = "ID del servicio") @PathVariable Long id) {
        return marketplaceService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Listar todos los servicios activos")
    @ApiResponse(responseCode = "200", description = "Lista de servicios")
    public ResponseEntity<List<ServiceResponse>> getAll() {
        List<ServiceResponse> services = marketplaceService.getAll();
        return ResponseEntity.ok(services);
    }

    @GetMapping("/offerer/{offererId}")
    @Operation(summary = "Listar servicios de un oferente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de servicios del oferente")
    })
    public ResponseEntity<List<ServiceResponse>> getByOffererId(
            @Parameter(description = "ID del oferente") @PathVariable Long offererId) {
        List<ServiceResponse> services = marketplaceService.getByOffererId(offererId);
        return ResponseEntity.ok(services);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un servicio")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Servicio actualizado"),
        @ApiResponse(responseCode = "404", description = "Servicio no encontrado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<ServiceResponse> update(
            @Parameter(description = "ID del servicio") @PathVariable Long id,
            @Valid @RequestBody ServiceRequest request) {
        ServiceResponse response = marketplaceService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un servicio (eliminación física)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Servicio eliminado"),
        @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID del servicio") @PathVariable Long id) {
        marketplaceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/soft-delete")
    @Operation(summary = "Marcar servicio como eliminado (soft delete)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Servicio marcado como eliminado"),
        @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    })
    public ResponseEntity<Void> softDelete(
            @Parameter(description = "ID del servicio") @PathVariable Long id) {
        marketplaceService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activar un servicio")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Servicio activado"),
        @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    })
    public ResponseEntity<Void> activate(
            @Parameter(description = "ID del servicio") @PathVariable Long id) {
        marketplaceService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desactivar un servicio")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Servicio desactivado"),
        @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    })
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "ID del servicio") @PathVariable Long id) {
        marketplaceService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
