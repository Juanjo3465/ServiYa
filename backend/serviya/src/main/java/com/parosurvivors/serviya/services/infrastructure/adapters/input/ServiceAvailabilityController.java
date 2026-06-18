package com.parosurvivors.serviya.services.infrastructure.adapters.input;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.parosurvivors.serviya.services.application.ports.input.ServiceAvailabilityServicePort;
import com.parosurvivors.serviya.services.infrastructure.adapters.input.api.ServiceAvailabilityApi;
import com.parosurvivors.serviya.services.infrastructure.dto.form.CreateServiceAvailabilityForm;
import com.parosurvivors.serviya.services.infrastructure.dto.form.UpdateServiceAvailabilityForm;
import com.parosurvivors.serviya.services.infrastructure.dto.response.ServiceAvailabilityResponse;
import com.parosurvivors.serviya.services.infrastructure.mappers.ServiceAvailabilityWebMapper;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/service-availabilities")
@RequiredArgsConstructor
public class ServiceAvailabilityController implements ServiceAvailabilityApi{

    private final ServiceAvailabilityServicePort serviceAvailabilityService;
    private final ServiceAvailabilityWebMapper mapper;
    
    @Override
    @PostMapping()
    public ResponseEntity<ServiceAvailabilityResponse> create(@Valid @RequestBody CreateServiceAvailabilityForm form) {
        
        ServiceAvailabilityResponse response = mapper.toResponse(
            serviceAvailabilityService.create(mapper.toCommand(form, currentServiceId()))
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<Void> delete(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public ResponseEntity<ServiceAvailabilityResponse> update(Long id, UpdateServiceAvailabilityForm form) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

    @Override
    @GetMapping("/service/{id}")
    public ResponseEntity<List<ServiceAvailabilityResponse>> getByServiceId(@Parameter(description = "ID del servicio")@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponses(serviceAvailabilityService.getByServiceId(id)));
    }
    
     /** TODO: reemplazar por el id extraido del JWT autenticado (Spring Security aun no configurado). */
    private Long currentServiceId() {
        return 0L;
    }
}
