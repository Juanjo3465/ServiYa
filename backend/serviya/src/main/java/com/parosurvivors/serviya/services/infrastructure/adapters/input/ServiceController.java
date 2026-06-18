package com.parosurvivors.serviya.services.infrastructure.adapters.input;


import com.parosurvivors.serviya.services.application.dto.query.SearchServiceQuery;
import com.parosurvivors.serviya.services.application.ports.input.MarketplaceCategoryPort;
import com.parosurvivors.serviya.services.application.ports.input.MarketplaceServicePort;
import com.parosurvivors.serviya.services.infrastructure.adapters.input.api.ServiceApi;
import com.parosurvivors.serviya.services.infrastructure.dto.form.CreateServiceForm;
import com.parosurvivors.serviya.services.infrastructure.dto.form.UpdateServiceForm;
import com.parosurvivors.serviya.services.infrastructure.dto.response.ServiceDetailResponse;
import com.parosurvivors.serviya.services.infrastructure.dto.response.ServiceResponse;
import com.parosurvivors.serviya.services.infrastructure.mappers.ServiceWebMapper;
import com.parosurvivors.serviya.services.infrastructure.mappers.ReviewWebMapper;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Adaptador de entrada (REST) de servicios. Placeholder funcional: enruta, mapea Form->Command y
 * dominio->Response (ServiceWebMapper) y delega en {@link MarketplaceServicePort}. La documentacion
 * vive en {@link ServiceApi}. Cubre la parte de gestion de MarketplaceService; los servicios del
 * modulo-3 (busqueda, categorias, horario) aun no tienen puerto (ver NOTAS.txt).
 */
@RestController
@RequiredArgsConstructor
public class ServiceController implements ServiceApi {

    private final MarketplaceServicePort marketplaceService;
    private final MarketplaceCategoryPort marketplaceCategoryPort;
    private final ServiceWebMapper mapper;
    private final ReviewWebMapper reviewMapper;

    @Override
    @PostMapping("/api/v1/services")
    public ResponseEntity<ServiceResponse> create(@Valid @RequestBody CreateServiceForm form) {
        ServiceResponse response = mapper.toResponse(
                marketplaceService.create(mapper.toCommand(form, currentOffererId())));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @GetMapping("/api/v1/services/{id}")
    public ResponseEntity<ServiceResponse> getById(
            @Parameter(description = "ID del servicio") @PathVariable Long id) {
        return marketplaceService.getById(id)
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @GetMapping("/api/v1/services/{id}/detail")
    public ResponseEntity<ServiceDetailResponse> getDetail(
            @Parameter(description = "ID del servicio") @PathVariable Long id) {
        return marketplaceService.getDetailById(id)
                .map(mapper::toDetailResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @GetMapping("/api/v1/services")
    public ResponseEntity<List<ServiceResponse>> getAll() {
        return ResponseEntity.ok(mapper.toResponses(marketplaceService.getAll()));
    }

    @Override
    @GetMapping("/api/v1/offerers/{offererId}/services")
    public ResponseEntity<List<ServiceResponse>> getByOffererId(
            @Parameter(description = "ID del oferente") @PathVariable Long offererId) {
        return ResponseEntity.ok(mapper.toResponses(marketplaceService.getByOffererId(offererId)));
    }

    @Override
    @GetMapping("/api/v1/services/search")
    public ResponseEntity<Page<ServiceResponse>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long offererId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxRating,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double maxDistanceKm,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        SearchServiceQuery criteria = SearchServiceQuery.builder()
                .name(name)
                .categoryId(categoryId)
                .offererId(offererId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .available(available)
                .minRating(minRating)
                .maxRating(maxRating)
                .latitude(latitude)
                .longitude(longitude)
                .maxDistanceKm(maxDistanceKm)
                .build();
        Page<ServiceResponse> page = marketplaceService.search(criteria, pageable)
                .map(mapper::toResponse);
        return ResponseEntity.ok(page);
    }

    @Override
    @PatchMapping("/api/v1/services/{id}")
    public ResponseEntity<ServiceResponse> update(
            @Parameter(description = "ID del servicio") @PathVariable Long id,
            @Valid @RequestBody UpdateServiceForm form) {
        return ResponseEntity.ok(mapper.toResponse(
                marketplaceService.update(mapper.toCommand(form, id))));
    }

    @Override
    @DeleteMapping("/api/v1/services/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID del servicio") @PathVariable Long id) {
        marketplaceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/api/v1/services/{id}/soft-delete")
    public ResponseEntity<Void> softDelete(
            @Parameter(description = "ID del servicio") @PathVariable Long id) {
        marketplaceService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/api/v1/services/{id}/activate")
    public ResponseEntity<Void> activate(
            @Parameter(description = "ID del servicio") @PathVariable Long id) {
        marketplaceService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/api/v1/services/{id}/deactivate")
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "ID del servicio") @PathVariable Long id) {
        marketplaceService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /** TODO: reemplazar por el id extraido del JWT autenticado (Spring Security aun no configurado). */
    private Long currentOffererId() {
        return 0L;
    }
}
